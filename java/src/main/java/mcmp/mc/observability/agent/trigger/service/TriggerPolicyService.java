package mcmp.mc.observability.agent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.agent.trigger.model.ManageTriggerTargetStorageInfo;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyCreateDto;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyUpdateDto;
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

    public ResBody<TriggerPolicyInfo> getDetail(ResBody<TriggerPolicyInfo> resBody, Long seq) {
        TriggerPolicyInfo triggerHistoryInfo = getDetail(seq);
        if( triggerHistoryInfo == null ) {
            resBody.setCode(ResultCode.INVALID_REQUEST);
            return resBody;
        }

        resBody.setData(triggerHistoryInfo);
        return resBody;
    }

    public TriggerPolicyInfo getDetail(Long seq) {
        TriggerPolicyInfo info = triggerPolicyMapper.getDetail(seq);
        return info;
    }

    public ResBody<Void> createPolicy(TriggerPolicyCreateDto dto) {
        ResBody<Void> resBody = new ResBody<>();
        TriggerPolicyInfo info = new TriggerPolicyInfo();
        info.setCreateDto(dto);
        try {
            if (info.getName() == null || info.getName().isEmpty()) {
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Trigger Policy Name is null/empty");
            }
            info.makeTickScript(info);
            int result = triggerPolicyMapper.createPolicy(info);
            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Trigger Policy insert error QueryResult={}", result);
            }

        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> updatePolicy(TriggerPolicyUpdateDto dto) {
        ResBody<Void> resBody = new ResBody<>();
        TriggerPolicyInfo info = triggerPolicyMapper.getDetail(dto.getSeq());
        info.setUpdateDto(dto);
        info.makeTickScript(info);

        try {
            int result = triggerPolicyMapper.updatePolicy(info);

            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Trigger Policy Update None");
            }

            if(hasNonNullFields(dto)) {
                List<ManageTriggerTargetStorageInfo> targetStorageInfoList = triggerTargetStorageMapper.getManageTriggerTargetStorageInfoList(Collections.singletonMap("policySeq", info.getSeq()));
                if (CollectionUtils.isEmpty(targetStorageInfoList))
                    return resBody;

                for(ManageTriggerTargetStorageInfo targetStorageInfo : targetStorageInfoList) {
                    kapacitorApiService.updateTask(info, targetStorageInfo);
                }
            }

        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
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
        ResBody<Void> resBody = new ResBody<>();
        try {
            if( seq <= 0 )
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Trigger Policy Sequence Error");

            TriggerPolicyInfo policyInfo = getDetail(seq);
            if(policyInfo == null)
                throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

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
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }
}
