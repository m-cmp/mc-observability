package mcmp.mc.observability.trigger.mapper;

import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.trigger.model.TriggerTargetInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TriggerTargetMapper {
    Long getListCount(PageableReqBody<TriggerTargetInfo> reqBody);

    List<TriggerTargetInfo> getList(PageableReqBody<TriggerTargetInfo> reqBody);

    List<TriggerTargetInfo> getList(Long policySeq);

    TriggerTargetInfo getDetail(Long seq);

}
