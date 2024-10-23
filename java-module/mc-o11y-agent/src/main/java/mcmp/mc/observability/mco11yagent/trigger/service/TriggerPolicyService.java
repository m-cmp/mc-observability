package mcmp.mc.observability.mco11yagent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.trigger.exception.TriggerResultCodeException;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.mco11yagent.trigger.model.ManageTriggerTargetStorageInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerPolicyCreateDto;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerPolicyUpdateDto;
import mcmp.mc.observability.mco11yagent.trigger.util.TimeConverterUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;


import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TriggerPolicyService {

    private final TriggerPolicyMapper triggerPolicyMapper;
    private final TriggerTargetMapper triggerTargetMapper;
    private final TriggerTargetStorageMapper triggerTargetStorageMapper;
    private final KapacitorApiService kapacitorApiService;

    @Value("${agent_manager_ip:http://localhost:18180}")
    private String agentManagerIp;

    public List<TriggerPolicyInfo> getList() {
        return triggerPolicyMapper.getList().stream()
                .peek(this::formatTriggerPolicyInfo)
                .collect(Collectors.toList());
    }

    public ResBody<TriggerPolicyInfo> getDetail(ResBody<TriggerPolicyInfo> ResBody, Long seq) {
        TriggerPolicyInfo triggerHistoryInfo = getDetail(seq);
        if (triggerHistoryInfo == null) {
            ResBody.setCode(ResultCode.INVALID_REQUEST);
            return ResBody;
        }

        ResBody.setData(triggerHistoryInfo);
        return ResBody;
    }

    public TriggerPolicyInfo getDetail(Long seq) {
        TriggerPolicyInfo info = triggerPolicyMapper.getDetail(seq);
        formatTriggerPolicyInfo(info);
        return info;
    }

    private void formatTriggerPolicyInfo(TriggerPolicyInfo policy) {
        if (policy.getCreateAt() != null) {
            policy.setCreateAt(TimeConverterUtils.toUTCFormat(policy.getCreateAt()));
        }
        if (policy.getUpdateAt() != null) {
            policy.setUpdateAt(TimeConverterUtils.toUTCFormat(policy.getUpdateAt()));
        }
    }

    public ResBody<Void> createPolicy(TriggerPolicyCreateDto dto) {
        ResBody<Void> ResBody = new ResBody<>();
        TriggerPolicyInfo info = new TriggerPolicyInfo();
        info.setAgentManagerIp(agentManagerIp);
        info.setCreateDto(dto);
        try {
            if (info.getName() == null || info.getName().isEmpty()) {
                throw new TriggerResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Trigger Policy Name is null/empty");
            }
            info.makeTickScript(info);
            int result = triggerPolicyMapper.createPolicy(info);
            if (result <= 0) {
                throw new TriggerResultCodeException(ResultCode.INVALID_ERROR, "Trigger Policy insert error QueryResult={}", result);
            }

        } catch (TriggerResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            ResBody.setCode(e.getResultCode());
        }
        return ResBody;
    }

    public ResBody<Void> updatePolicy(TriggerPolicyUpdateDto dto) {
        ResBody<Void> ResBody = new ResBody<>();
        TriggerPolicyInfo info = triggerPolicyMapper.getDetail(dto.getSeq());
        info.setAgentManagerIp(agentManagerIp);
        info.setUpdateDto(dto);
        info.makeTickScript(info);

        try {
            int result = triggerPolicyMapper.updatePolicy(info);

            if (result <= 0) {
                throw new TriggerResultCodeException(ResultCode.INVALID_ERROR, "Trigger Policy Update None");
            }

            if (hasNonNullFields(dto)) {
                List<ManageTriggerTargetStorageInfo> targetStorageInfoList = triggerTargetStorageMapper.getManageTriggerTargetStorageInfoList(Collections.singletonMap("policySeq", info.getSeq()));
                if (CollectionUtils.isEmpty(targetStorageInfoList))
                    return ResBody;

                for (ManageTriggerTargetStorageInfo targetStorageInfo : targetStorageInfoList) {
                    kapacitorApiService.updateTask(info, targetStorageInfo);
                }
            }

        } catch (TriggerResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            ResBody.setCode(e.getResultCode());
        }
        return ResBody;
    }

    private boolean hasNonNullFields(TriggerPolicyUpdateDto dto) {
        List<Object> fields = Arrays.asList(
                dto.getMetric(),
                dto.getField(),
                dto.getGroupFields(),
                dto.getStatistics(),
                dto.getThreshold(),
                dto.getStatus()
        );
        return fields.stream().anyMatch(field -> field != null);
    }

    public ResBody<Void> deletePolicy(Long seq) {
        ResBody<Void> ResBody = new ResBody<>();
        try {
            if (seq <= 0)
                throw new TriggerResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Trigger Policy Sequence Error");

            TriggerPolicyInfo policyInfo = getDetail(seq);
            if (policyInfo == null)
                throw new TriggerResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

            List<ManageTriggerTargetStorageInfo> targetStorageInfoList = triggerTargetStorageMapper.getManageTriggerTargetStorageInfoList(Collections.singletonMap("policySeq", seq));
            if (!CollectionUtils.isEmpty(targetStorageInfoList)) {
                for (ManageTriggerTargetStorageInfo targetStorageInfo : targetStorageInfoList) {
                    try {
                        kapacitorApiService.deleteTask(seq, targetStorageInfo.getUrl());
                    } catch (Exception e) {
                        log.error("Failed to delete Kapacitor task.");
                    }
                }
            }

            triggerTargetStorageMapper.deleteTriggerTargetStorageByPolicySeq(seq);
            triggerTargetMapper.deleteTriggerTargetByPolicySeq(seq);
            triggerPolicyMapper.deletePolicy(seq);
        } catch (TriggerResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            ResBody.setCode(e.getResultCode());
        }
        return ResBody;
    }
}
