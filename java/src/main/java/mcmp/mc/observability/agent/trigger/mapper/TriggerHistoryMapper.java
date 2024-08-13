package mcmp.mc.observability.agent.trigger.mapper;

import mcmp.mc.observability.agent.common.model.PageableReqBody;
import mcmp.mc.observability.agent.trigger.model.TriggerHistoryInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TriggerHistoryMapper {
    Long getListCount(PageableReqBody<TriggerHistoryInfo> reqBody);

    List<TriggerHistoryInfo> getList(PageableReqBody<TriggerHistoryInfo> reqBody);

    TriggerHistoryInfo getDetail(Long seq);
}
