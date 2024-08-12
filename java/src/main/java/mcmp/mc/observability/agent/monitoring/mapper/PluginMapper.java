package mcmp.mc.observability.agent.monitoring.mapper;

import mcmp.mc.observability.agent.monitoring.model.PluginDefInfo;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface PluginMapper {
    @MapKey("seq")
    Map<Long, PluginDefInfo> getAllPlugin();
}