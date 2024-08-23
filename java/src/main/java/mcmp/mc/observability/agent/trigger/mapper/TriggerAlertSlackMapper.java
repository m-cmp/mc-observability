package mcmp.mc.observability.agent.trigger.mapper;

import mcmp.mc.observability.agent.trigger.model.TriggerSlackUserInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TriggerAlertSlackMapper {

    List<TriggerSlackUserInfo> getSlackUserListByPolicySeq(Long policySeq);

    int createSlackUser(TriggerSlackUserInfo info);

    int deleteSlackUser(Long seq);

    TriggerSlackUserInfo getSlackUser(Long seq);
}
