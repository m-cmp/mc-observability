package mcmp.mc.observability.trigger.mapper;

import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.trigger.model.TriggerPolicyInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TriggerPolicyMapper {

    TriggerPolicyInfo getDetail(Long seq);

    List<TriggerPolicyInfo> getList(PageableReqBody<TriggerPolicyInfo> reqBody);

    Long getListCount(PageableReqBody<TriggerPolicyInfo> reqBody);

    int updatePolicy(TriggerPolicyInfo info);

    void deletePolicy(Long seq);
}
