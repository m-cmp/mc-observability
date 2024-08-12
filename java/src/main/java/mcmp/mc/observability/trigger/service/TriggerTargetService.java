package mcmp.mc.observability.trigger.service;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.trigger.mapper.TriggerTargetMapper;
import mcmp.mc.observability.trigger.model.TriggerPolicyInfo;
import mcmp.mc.observability.trigger.model.TriggerTargetInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TriggerTargetService {

    private final TriggerPolicyMapper triggerPolicyMapper;
    private final TriggerTargetMapper triggerTargetMapper;

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

        List<TriggerTargetInfo> targetInfoList = triggerTargetMapper.getList(policySeq);
        return resBody;
    }
}
