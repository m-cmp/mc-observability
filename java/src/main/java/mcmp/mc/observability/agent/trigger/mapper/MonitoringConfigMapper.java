package mcmp.mc.observability.agent.trigger.mapper;

import mcmp.mc.observability.agent.trigger.model.MonitoringConfigInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface MonitoringConfigMapper {

    List<MonitoringConfigInfo> getHostStorageList(Map<String, Object> params);

}
