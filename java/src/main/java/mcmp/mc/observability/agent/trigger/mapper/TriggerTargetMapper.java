package mcmp.mc.observability.agent.trigger.mapper;

import mcmp.mc.observability.agent.trigger.model.TriggerTargetInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface TriggerTargetMapper {

    List<TriggerTargetInfo> getList(Long policySeq);

    List<TriggerTargetInfo> getListByPolicySeq(Long policySeq);

    TriggerTargetInfo getDetail(Long seq);

    TriggerTargetInfo getTargetDetail(Map<String, Object> params);

    int createTarget(TriggerTargetInfo triggerTargetInfo);

    int deleteTriggerTargetBySeq(Long seq);

    void deleteTriggerTargetByPolicySeq(Long seq);
}
