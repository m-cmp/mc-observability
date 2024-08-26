package mcmp.mc.observability.agent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.monitoring.model.InfluxDBConnector;
import mcmp.mc.observability.agent.trigger.mapper.MonitoringConfigMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.agent.trigger.model.MonitoringConfigInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerTargetInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerTargetStorageInfo;
import mcmp.mc.observability.agent.trigger.model.dto.ManageTriggerTargetDto;
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
    private final MonitoringConfigMapper monitoringConfigMapper;
    private final KapacitorApiService kapacitorApiService;

    public List<TriggerTargetInfo> getList(Long policySeq) {
        TriggerPolicyInfo triggerPolicyInfo = triggerPolicyMapper.getDetail(policySeq);
        if(triggerPolicyInfo == null)
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

        return triggerTargetMapper.getList(policySeq);
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

    public ResBody<Void> setTargets(Long policySeq, List<ManageTriggerTargetDto> targets) {
        ResBody<Void> resBody = new ResBody<>();
        TriggerPolicyInfo policyInfo = triggerPolicyMapper.getDetail(policySeq);
        if (policyInfo == null)
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

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
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Set Trigger Target failed");
        }

        return resBody;
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
            List<MonitoringConfigInfo> hostStorageInfoList = monitoringConfigMapper.getHostStorageList(params);
            if (CollectionUtils.isEmpty(hostStorageInfoList))
                continue;

            for (MonitoringConfigInfo info : hostStorageInfoList) {
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
