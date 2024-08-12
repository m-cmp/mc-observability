package mcmp.mc.observability.agent.monitoring.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.monitoring.mapper.PluginMapper;
import mcmp.mc.observability.agent.monitoring.model.PluginDefInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@DependsOn({"pluginMapper"})
@RequiredArgsConstructor
public class PluginLoader {

    private final PluginMapper pluginMapper;
    private Map<Long, PluginDefInfo> pluginMap = new HashMap<>();

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @PostConstruct
    private void init() {
        if( datasourceUrl.contains(Constants.EMPTY_HOST) ) return;
        pluginMap = pluginMapper.getAllPlugin();
    }

    public void refresh() {
        init();
    }

    public PluginDefInfo getPluginDefInfo(Long seq) {
        return pluginMap.get(seq);
    }

    public List<PluginDefInfo> getPluginDefList() {
        return new ArrayList<>(pluginMap.values());
    }

    public List<PluginDefInfo> getPluginDefList(String type) {
        return pluginMap.values().stream().filter(f -> f.getPluginType().equals(type)).collect(Collectors.toList());
    }
}