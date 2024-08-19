package mcmp.mc.observability.agent.trigger.mapper;

import mcmp.mc.observability.agent.trigger.model.TriggerTargetStorageInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface TriggerTargetStorageMapper {
    int createTargetStorage(TriggerTargetStorageInfo targetStorageInfo);

    List<Map<String, Object>> getRemainTaskStorageCount(Long policySeq);

    int deleteTriggerTargetStorageByTargetSeq(Long targetSeq);
}
