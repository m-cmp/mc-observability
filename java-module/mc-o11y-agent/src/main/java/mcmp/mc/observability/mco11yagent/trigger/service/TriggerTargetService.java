package mcmp.mc.observability.mco11yagent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.trigger.exception.TriggerResultCodeException;
import mcmp.mc.observability.mco11yagent.trigger.mapper.MonitoringConfigStorageMapper;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerMonitoringConfigInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerResBody;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.monitoring.model.InfluxDBConnector;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerTargetInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerTargetStorageInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.ManageTriggerTargetDto;
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
    private final MonitoringConfigStorageMapper monitoringConfigStorageMapper;
    private final KapacitorApiService kapacitorApiService;

    public List<TriggerTargetInfo> getList(Long policySeq) {
        TriggerPolicyInfo triggerPolicyInfo = triggerPolicyMapper.getDetail(policySeq);
        if(triggerPolicyInfo == null)
            throw new TriggerResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

        return triggerTargetMapper.getList(policySeq);
    }

    public TriggerResBody<TriggerTargetInfo> getDetail(TriggerResBody<TriggerTargetInfo> triggerResBody, Long seq) {
        TriggerTargetInfo triggerTargetInfo = getDetail(seq);
        if( triggerTargetInfo == null ) {
            triggerResBody.setCode(ResultCode.INVALID_REQUEST);
            return triggerResBody;
        }

        triggerResBody.setData(triggerTargetInfo);
        return triggerResBody;
    }

    public TriggerTargetInfo getDetail(Long seq) {
        TriggerTargetInfo info = triggerTargetMapper.getDetail(seq);
        return info;
    }

    public TriggerResBody<Void> setTargets(Long policySeq, List<ManageTriggerTargetDto> targets) {
        TriggerResBody<Void> triggerResBody = new TriggerResBody<>();
        TriggerPolicyInfo policyInfo = triggerPolicyMapper.getDetail(policySeq);
        if (policyInfo == null)
            throw new TriggerResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

        List<TriggerTargetInfo> targetInfoList = triggerTargetMapper.getListByPolicySeq(policySeq);
        List<ManageTriggerTargetDto> addTargetList = new ArrayList<>();

        if(!CollectionUtils.isEmpty(targets)) {
            addTargetList.addAll(targets);
            if(!CollectionUtils.isEmpty(targetInfoList)) {
                addTargetList.removeIf(a -> targetInfoList.stream().anyMatch(b -> b.getTargetId().equals(a.getTargetId()) && b.getNsId().equals(a.getNsId())));
                targetInfoList.removeIf(a -> targets.stream().anyMatch(b -> b.getTargetId().equals(a.getTargetId()) && b.getNsId().equals(a.getNsId())));
            }
        }

        try {
            addTriggerTargets(addTargetList, policyInfo);
            deleteTriggerTargets(targetInfoList, policyInfo);
        } catch (Exception e) {
            throw new TriggerResultCodeException(ResultCode.INVALID_REQUEST, "Set Trigger Target failed");
        }

        return triggerResBody;
    }

    private void addTriggerTargets(List<ManageTriggerTargetDto> addTargetList, TriggerPolicyInfo policyInfo) {
        if (CollectionUtils.isEmpty(addTargetList))
            return;

        Map<String, Object> params = new HashMap<>();
        for (ManageTriggerTargetDto targetDto : addTargetList) {
            TriggerTargetInfo triggerTargetInfo = TriggerTargetInfo.builder()
                    .policySeq(policyInfo.getSeq())
                    .nsId(targetDto.getNsId())
                    .targetId(targetDto.getTargetId())
                    .build();

            triggerTargetMapper.createTarget(triggerTargetInfo);
            Long seq = triggerTargetInfo.getSeq();
            triggerTargetInfo.setSeq(seq);

            params.put("pluginName", "influxdb");
            params.put("notState", "DELETE");
            params.put("targetId", targetDto.getTargetId());
            params.put("nsId", targetDto.getNsId());
            List<TriggerMonitoringConfigInfo> hostStorageInfoList = monitoringConfigStorageMapper.getHostStorageList(params);
            if (CollectionUtils.isEmpty(hostStorageInfoList))
                continue;

            for (TriggerMonitoringConfigInfo info : hostStorageInfoList) {
                InfluxDBConnector influxDBConnector = new InfluxDBConnector(info.getConfig());
                TriggerTargetStorageInfo targetStorageInfo = TriggerTargetStorageInfo.builder()
                        .targetSeq(triggerTargetInfo.getSeq())
                        .url(influxDBConnector.getUrl())
                        .database(influxDBConnector.getDatabase())
                        .retentionPolicy(influxDBConnector.getRetentionPolicy())
                        .build();

                kapacitorApiService.createTask(policyInfo, targetStorageInfo.getUrl(), targetStorageInfo.getDatabase(), targetStorageInfo.getRetentionPolicy());
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
                        log.error("Failed to delete task. Error : {}, TaskId : {}", e.getMessage(), targetInfo.getPolicySeq());
                    }
                }
            }
        }
    }
}
