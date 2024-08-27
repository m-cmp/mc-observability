package mcmp.mc.observability.mco11yagent.trigger.mapper;

import mcmp.mc.observability.mco11yagent.trigger.model.PageableReqBody;
import mcmp.mc.observability.mco11yagent.trigger.model.TriggerHistoryInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TriggerHistoryMapper {

    List<TriggerHistoryInfo> getList(Long policySeq);

    TriggerHistoryInfo getDetail(Long seq);

    void createHistory(TriggerHistoryInfo historyInfo);
}
