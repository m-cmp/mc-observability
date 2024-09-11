package mcmp.mc.observability.mco11yagent.trigger.mapper;

import mcmp.mc.observability.mco11yagent.trigger.model.ManageTriggerTargetStorageInfo;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerTargetStorageInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface TriggerTargetStorageMapper {
    int createTargetStorage(TriggerTargetStorageInfo targetStorageInfo);

    List<Map<String, Object>> getRemainTaskStorageCount(Long policySeq);

    int deleteTriggerTargetStorageByTargetSeq(Long targetSeq);

    List<ManageTriggerTargetStorageInfo> getManageTriggerTargetStorageInfoList(Map<String, Object> params);

    void deleteTriggerTargetStorageByPolicySeq(Long seq);

    List<Long> getPolicySeqListRegisteredInStorage(Map<String, String> url);

    TriggerTargetStorageInfo getStorageInfo(Map<String, Object> params);

    Long getUsageStorageCount(Map<String, Object> params);

    void updateTargetStorage(TriggerTargetStorageInfo targetStorageInfo);

    void deleteTriggerTargetStorage(TriggerTargetStorageInfo targetStorageInfo);
}
