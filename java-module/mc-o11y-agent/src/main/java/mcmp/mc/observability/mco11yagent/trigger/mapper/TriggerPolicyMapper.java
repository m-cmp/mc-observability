package mcmp.mc.observability.mco11yagent.trigger.mapper;

import mcmp.mc.observability.mco11yagent.trigger.model.PageableReqBody;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerPolicyInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface TriggerPolicyMapper {

    TriggerPolicyInfo getDetail(Long seq);

    List<TriggerPolicyInfo> getList();

    int createPolicy(TriggerPolicyInfo info);

    int updatePolicy(TriggerPolicyInfo info);

    void deletePolicy(Long seq);
}
