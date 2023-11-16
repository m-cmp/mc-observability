package mcmp.mc.observability.agent.mapper;

import mcmp.mc.observability.agent.model.PluginDefInfo;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface PluginMapper {
    @MapKey("seq")
    Map<Long, PluginDefInfo> getAllPlugin();
}