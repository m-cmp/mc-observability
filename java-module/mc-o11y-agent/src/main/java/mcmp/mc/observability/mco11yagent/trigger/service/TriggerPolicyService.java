package mcmp.mc.observability.mco11yagent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.trigger.exception.TriggerResultCodeException;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerResBody;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.mco11yagent.trigger.model.ManageTriggerTargetStorageInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerPolicyCreateDto;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerPolicyUpdateDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TriggerPolicyService {

    private final TriggerPolicyMapper triggerPolicyMapper;
    private final TriggerTargetMapper triggerTargetMapper;
    private final TriggerTargetStorageMapper triggerTargetStorageMapper;
    private final KapacitorApiService kapacitorApiService;

    public List<TriggerPolicyInfo> getList() {
        return triggerPolicyMapper.getList();
    }

    public TriggerResBody<TriggerPolicyInfo> getDetail(TriggerResBody<TriggerPolicyInfo> triggerResBody, Long seq) {
        TriggerPolicyInfo triggerHistoryInfo = getDetail(seq);
        if( triggerHistoryInfo == null ) {
            triggerResBody.setCode(ResultCode.INVALID_REQUEST);
            return triggerResBody;
        }

        triggerResBody.setData(triggerHistoryInfo);
        return triggerResBody;
    }

    public TriggerPolicyInfo getDetail(Long seq) {
        TriggerPolicyInfo info = triggerPolicyMapper.getDetail(seq);
        return info;
    }

    public TriggerResBody<Void> createPolicy(TriggerPolicyCreateDto dto) {
        TriggerResBody<Void> triggerResBody = new TriggerResBody<>();
        TriggerPolicyInfo info = new TriggerPolicyInfo();
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
            triggerResBody.setCode(e.getResultCode());
        }
        return triggerResBody;
    }

    public TriggerResBody<Void> updatePolicy(TriggerPolicyUpdateDto dto) {
        TriggerResBody<Void> triggerResBody = new TriggerResBody<>();
        TriggerPolicyInfo info = triggerPolicyMapper.getDetail(dto.getSeq());
        info.setUpdateDto(dto);
        info.makeTickScript(info);

        try {
            int result = triggerPolicyMapper.updatePolicy(info);

            if (result <= 0) {
                throw new TriggerResultCodeException(ResultCode.INVALID_ERROR, "Trigger Policy Update None");
            }

            if(hasNonNullFields(dto)) {
                List<ManageTriggerTargetStorageInfo> targetStorageInfoList = triggerTargetStorageMapper.getManageTriggerTargetStorageInfoList(Collections.singletonMap("policySeq", info.getSeq()));
                if (CollectionUtils.isEmpty(targetStorageInfoList))
                    return triggerResBody;

                for(ManageTriggerTargetStorageInfo targetStorageInfo : targetStorageInfoList) {
                    kapacitorApiService.updateTask(info, targetStorageInfo);
                }
            }

        } catch (TriggerResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            triggerResBody.setCode(e.getResultCode());
        }
        return triggerResBody;
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

    public TriggerResBody<Void> deletePolicy(Long seq) {
        TriggerResBody<Void> triggerResBody = new TriggerResBody<>();
        try {
            if( seq <= 0 )
                throw new TriggerResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Trigger Policy Sequence Error");

            TriggerPolicyInfo policyInfo = getDetail(seq);
            if(policyInfo == null)
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
        }
        catch (TriggerResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            triggerResBody.setCode(e.getResultCode());
        }
        return triggerResBody;
    }
}
