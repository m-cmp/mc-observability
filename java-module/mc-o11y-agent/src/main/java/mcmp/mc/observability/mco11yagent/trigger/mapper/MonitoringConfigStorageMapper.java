package mcmp.mc.observability.mco11yagent.trigger.mapper;

import mcmp.mc.observability.mco11yagent.trigger.model.TriggerMonitoringConfigInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface MonitoringConfigStorageMapper {

    List<TriggerMonitoringConfigInfo> getHostStorageList(Map<String, Object> params);

}
