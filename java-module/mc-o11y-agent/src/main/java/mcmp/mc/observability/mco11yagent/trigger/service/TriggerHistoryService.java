package mcmp.mc.observability.mco11yagent.trigger.service;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.trigger.exception.TriggerResultCodeException;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerHistoryMapper;
import mcmp.mc.observability.mco11yagent.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerHistoryInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerPolicyInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TriggerHistoryService {

    private final TriggerPolicyMapper triggerPolicyMapper;
    private final TriggerHistoryMapper triggerHistoryMapper;

    public List<TriggerHistoryInfo> getList(Long policySeq) {
        TriggerPolicyInfo triggerPolicyInfo = triggerPolicyMapper.getDetail(policySeq);
        if(triggerPolicyInfo == null)
            throw new TriggerResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

        return triggerHistoryMapper.getList(policySeq);
    }

    public ResBody<TriggerHistoryInfo> getDetail(ResBody<TriggerHistoryInfo> ResBody, Long seq) {
        TriggerHistoryInfo triggerHistoryInfo = getDetail(seq);
        if( triggerHistoryInfo == null ) {
            ResBody.setCode(ResultCode.INVALID_REQUEST);
            return ResBody;
        }

        ResBody.setData(triggerHistoryInfo);
        return ResBody;
    }

    public TriggerHistoryInfo getDetail(Long seq) {
        TriggerHistoryInfo info = triggerHistoryMapper.getDetail(seq);
        return info;
    }
}
