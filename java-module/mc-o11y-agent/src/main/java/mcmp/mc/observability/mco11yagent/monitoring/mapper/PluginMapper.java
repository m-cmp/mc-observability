package mcmp.mc.observability.mco11yagent.monitoring.mapper;

import mcmp.mc.observability.mco11yagent.monitoring.model.PluginDefInfo;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PluginMapper {
    @MapKey("seq")
    Map<Long, PluginDefInfo> getAllPlugin();
    List<PluginDefInfo> getList();
    PluginDefInfo getPlugin(@Param("seqId") Long seqId);
}
