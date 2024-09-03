package mcmp.mc.observability.mco11yagent.monitoring.mapper;

import mcmp.mc.observability.mco11yagent.monitoring.model.MonitoringConfigInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MonitoringConfigMapper {

    MonitoringConfigInfo getDetail(Long seq);
    List<MonitoringConfigInfo> getList(String nsId, String targetId);
    int insert(MonitoringConfigInfo monitoringConfigInfo);
    int update(MonitoringConfigInfo monitoringConfigInfo);
    int delete(MonitoringConfigInfo monitoringConfigInfo);
    int updateState(MonitoringConfigInfo monitoringConfigInfo);
}
