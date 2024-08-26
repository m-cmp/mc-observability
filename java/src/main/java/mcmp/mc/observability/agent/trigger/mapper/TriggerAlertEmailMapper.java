package mcmp.mc.observability.agent.trigger.mapper;

import mcmp.mc.observability.agent.trigger.model.TriggerEmailUserInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TriggerAlertEmailMapper {

    List<TriggerEmailUserInfo> getEmailUserListByPolicySeq(Long policySeq);

    int createEmailUser(TriggerEmailUserInfo info);

    int deleteEmailUser(Long seq);

    TriggerEmailUserInfo getEmailUser(Long seq);

}
