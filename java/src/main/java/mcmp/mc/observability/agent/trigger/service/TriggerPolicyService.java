package mcmp.mc.observability.agent.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.monitoring.model.HostStorageInfo;
import mcmp.mc.observability.agent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.agent.trigger.mapper.TriggerTargetStorageMapper;
import mcmp.mc.observability.agent.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyCreateDto;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyUpdateDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerPolicyService {

    private final TriggerPolicyMapper triggerPolicyMapper;
    private final TriggerTargetMapper triggerTargetMapper;
    private final TriggerTargetStorageMapper triggerTargetStorageMapper;

    public PageableResBody<TriggerPolicyInfo> getList(PageableReqBody<TriggerPolicyInfo> reqBody) {
        PageableResBody<TriggerPolicyInfo> result = new PageableResBody<>();
        result.setRecords(triggerPolicyMapper.getListCount(reqBody));

        if( result.getRecords() > 0 ) {
            List<TriggerPolicyInfo> list = triggerPolicyMapper.getList(reqBody);
            if( list == null ) list = new ArrayList<>();
            result.setRows(list);
        }

        return result;
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
            // TODO: update kapacitor task
            int result = triggerPolicyMapper.updatePolicy(info);

            if (result <= 0) {
                throw new ResultCodeException(ResultCode.INVALID_ERROR, "Trigger Policy Update None");
            }
        } catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }

    public ResBody<Void> deletePolicy(Long seq) {
        ResBody<Void> resBody = new ResBody<>();
        try {
            if( seq <= 0 )
                throw new ResultCodeException(ResultCode.NOT_FOUND_REQUIRED, "Trigger Policy Sequence Error");

            TriggerPolicyInfo policyInfo = getDetail(seq);
            if(policyInfo == null)
                throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

            // TODO: delete target, targetStorage (kapacitor task)
            triggerPolicyMapper.deletePolicy(seq);
        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }
}
