package mcmp.mc.observability.agent.trigger.mapper;

import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.trigger.model.TriggerTargetInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TriggerTargetMapper {
    Long getListCount(PageableReqBody<TriggerTargetInfo> reqBody);

    List<TriggerTargetInfo> getList(PageableReqBody<TriggerTargetInfo> reqBody);

    List<TriggerTargetInfo> getListPolicySeq(Long policySeq);

    TriggerTargetInfo getDetail(Long seq);

    int createTarget(TriggerTargetInfo triggerTargetInfo);

    int deleteTriggerTargetBySeq(Long seq);

    void deleteTriggerTargetByPolicySeq(Long seq);
}
