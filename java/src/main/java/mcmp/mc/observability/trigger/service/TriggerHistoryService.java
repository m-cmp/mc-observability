package mcmp.mc.observability.trigger.service;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.common.model.PageableResBody;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.trigger.mapper.TriggerHistoryMapper;
import mcmp.mc.observability.trigger.mapper.TriggerPolicyMapper;
import mcmp.mc.observability.trigger.model.TriggerHistoryInfo;
import mcmp.mc.observability.trigger.model.TriggerPolicyInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TriggerHistoryService {

    private final TriggerPolicyMapper triggerPolicyMapper;
    private final TriggerHistoryMapper triggerHistoryMapper;

    public PageableResBody<TriggerHistoryInfo> getList(PageableReqBody<TriggerHistoryInfo> reqBody) {
        TriggerPolicyInfo triggerPolicyInfo = triggerPolicyMapper.getDetail(reqBody.getData().getPolicySeq());
        if(triggerPolicyInfo == null)
            throw new ResultCodeException(ResultCode.INVALID_REQUEST, "Trigger Policy Sequence Error");

        PageableResBody<TriggerHistoryInfo> result = new PageableResBody<>();
        result.setRecords(triggerHistoryMapper.getListCount(reqBody));

        if( result.getRecords() > 0 ) {
            List<TriggerHistoryInfo> list = triggerHistoryMapper.getList(reqBody);
            if( list == null ) list = new ArrayList<>();
            result.setRows(list);
        }

        return result;
    }

    public ResBody<TriggerHistoryInfo> getDetail(ResBody<TriggerHistoryInfo> resBody, Long seq) {
        TriggerHistoryInfo triggerHistoryInfo = getDetail(seq);
        if( triggerHistoryInfo == null ) {
            resBody.setCode(ResultCode.INVALID_REQUEST);
            return resBody;
        }

        resBody.setData(triggerHistoryInfo);
        return resBody;
    }

    public TriggerHistoryInfo getDetail(Long seq) {
        TriggerHistoryInfo info = triggerHistoryMapper.getDetail(seq);
        return info;
    }
}
