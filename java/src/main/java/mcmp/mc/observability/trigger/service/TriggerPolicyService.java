package mcmp.mc.observability.trigger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.trigger.model.dto.TriggerPolicyUpdateDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerPolicyService {

    private final TriggerPolicyMapper triggerPolicyMapper;

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

    public ResBody<Void> updatePolicy(TriggerPolicyUpdateDto dto) {
        ResBody<Void> resBody = new ResBody<>();
        TriggerPolicyInfo info = new TriggerPolicyInfo();
        info.setUpdateDto(dto);

        try {
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

            triggerPolicyMapper.deletePolicy(seq);
        }
        catch (ResultCodeException e) {
            log.error(e.getMessage(), e.getObjects());
            resBody.setCode(e.getResultCode());
        }
        return resBody;
    }
}
