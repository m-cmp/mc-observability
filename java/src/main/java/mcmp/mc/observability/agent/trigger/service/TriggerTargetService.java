package mcmp.mc.observability.agent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.monitoring.mapper.HostStorageMapper;
import mcmp.mc.observability.agent.monitoring.model.HostStorageInfo;
import mcmp.mc.observability.agent.monitoring.model.InfluxDBConnector;
import mcmp.mc.observability.agent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerTargetInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerTargetStorageInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerTargetService {

    private final TriggerPolicyMapper triggerPolicyMapper;
    private final TriggerTargetMapper triggerTargetMapper;
    private final TriggerTargetStorageMapper triggerTargetStorageMapper;
    private final HostStorageMapper hostStorageMapper;
    private final KapacitorApiService kapacitorApiService;

    public PageableResBody<TriggerTargetInfo> getList(PageableReqBody<TriggerTargetInfo> reqBody) {
        TriggerPolicyInfo triggerPolicyInfo = triggerPolicyMapper.getDetail(reqBody.getData().getPolicySeq());
        if(triggerPolicyInfo == null)
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

        PageableResBody<TriggerTargetInfo> result = new PageableResBody<>();
        result.setRecords(triggerTargetMapper.getListCount(reqBody));

        if( result.getRecords() > 0 ) {
            List<TriggerTargetInfo> list = triggerTargetMapper.getList(reqBody);
            if( list == null ) list = new ArrayList<>();
            result.setRows(list);
        }

        return result;
    }

    public ResBody<TriggerTargetInfo> getDetail(ResBody<TriggerTargetInfo> resBody, Long seq) {
        TriggerTargetInfo triggerTargetInfo = getDetail(seq);
        if( triggerTargetInfo == null ) {
            resBody.setCode(ResultCode.INVALID_REQUEST);
            return resBody;
        }

        resBody.setData(triggerTargetInfo);
        return resBody;
    }

    public TriggerTargetInfo getDetail(Long seq) {
        TriggerTargetInfo info = triggerTargetMapper.getDetail(seq);
        return info;
    }

    public ResBody<Void> setTargets(Long policySeq, List<Long> hostSeqs) {
        ResBody<Void> resBody = new ResBody<>();
        TriggerPolicyInfo policyInfo = triggerPolicyMapper.getDetail(policySeq);
        if (policyInfo == null)
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

        List<TriggerTargetInfo> targetInfoList = triggerTargetMapper.getListPolicySeq(policySeq);
        List<Long> addTargetHostSeqList = new ArrayList<>();

        if(!CollectionUtils.isEmpty(hostSeqs)) {
            addTargetHostSeqList.addAll(hostSeqs);
            if(!CollectionUtils.isEmpty(targetInfoList)) {
                addTargetHostSeqList.removeIf(a -> targetInfoList.stream().anyMatch(b -> b.getHostSeq() == a));
                targetInfoList.removeIf(a -> hostSeqs.stream().anyMatch(b -> b == a.getHostSeq()));
            }
        }

        try {
            addTriggerTargets(addTargetHostSeqList, policyInfo);
            deleteTriggerTargets(targetInfoList, policyInfo);
        } catch (Exception e) {
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Set Trigger Target failed");
        }

        return resBody;
    }

    private void addTriggerTargets(List<Long> addTargetHostSeqList, TriggerPolicyInfo policyInfo) {
        if (CollectionUtils.isEmpty(addTargetHostSeqList))
            return;

        Map<String, Object> params = new HashMap<>();
        for (Long hostSeq : addTargetHostSeqList) {
            TriggerTargetInfo triggerTargetInfo = TriggerTargetInfo.builder()
                    .policySeq(policyInfo.getSeq())
                    .hostSeq(hostSeq)
                    .build();

            triggerTargetMapper.createTarget(triggerTargetInfo);
            Long seq = triggerTargetInfo.getSeq();
            triggerTargetInfo.setSeq(seq);

            params.put("pluginName", "influxdb");
            params.put("hostSeq", hostSeq);
            List<HostStorageInfo> hostStorageInfoList = hostStorageMapper.getHostStorageList(params);
            if (CollectionUtils.isEmpty(hostStorageInfoList))
                continue;

            for (HostStorageInfo info : hostStorageInfoList) {
                InfluxDBConnector influxDBConnector = new InfluxDBConnector(info.getSetting());
                TriggerTargetStorageInfo targetStorageInfo = TriggerTargetStorageInfo.builder()
                        .targetSeq(triggerTargetInfo.getSeq())
                        .url(influxDBConnector.getUrl())
                        .database(influxDBConnector.getDatabase())
                        .retentionPolicy(influxDBConnector.getRetentionPolicy())
                        .build();

                kapacitorApiService.createTask(policyInfo, targetStorageInfo);
                triggerTargetStorageMapper.createTargetStorage(targetStorageInfo);
            }
        }
    }

    private void deleteTriggerTargets(List<TriggerTargetInfo> targetInfoList, TriggerPolicyInfo policyInfo) {
        if (CollectionUtils.isEmpty(targetInfoList))
            return;

        for (TriggerTargetInfo targetInfo : targetInfoList) {
            List<Map<String, Object>> taskStorageCountList = triggerTargetStorageMapper.getRemainTaskStorageCount(policyInfo.getSeq());

            int result = triggerTargetStorageMapper.deleteTriggerTargetStorageByTargetSeq(targetInfo.getSeq());
            result += triggerTargetMapper.deleteTriggerTargetBySeq(targetInfo.getSeq());
            if (result == 0)
                continue;

            if (CollectionUtils.isEmpty(taskStorageCountList))
                continue;

            for (Map<String, Object> taskStorage : taskStorageCountList) {
                if(Integer.parseInt(String.valueOf(taskStorage.get("count"))) < 2) {
                    try {
                        kapacitorApiService.deleteTask(targetInfo.getPolicySeq(), String.valueOf(taskStorage.get("url")));
                    } catch (Exception e) {

                    }
                }
            }
        }
    }
}
