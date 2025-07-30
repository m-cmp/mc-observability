package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.entity.AgentPluginDefEntity;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegrafConfigService {

    private final TumblebugService tumblebugService;
    private final AgentPluginDefService agentPluginDefService;
    
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\[\\[inputs\\.(\\w+)]]");
    private static final String TELEGRAF_CONFIG_PATH = "/etc/telegraf/telegraf.conf";

    public List<MonitoringItemDTO> getTelegrafItems(String nsId, String mciId, String targetId, String userName) {
        try {
            if (!tumblebugService.isConnectedVM(nsId, mciId, targetId, userName)) {
                log.warn("VM not connected for target: {}/{}/{}", nsId, mciId, targetId);
                return List.of();
            }

            String configContent = tumblebugService.executeCommand(nsId, mciId, targetId, userName, 
                "cat " + TELEGRAF_CONFIG_PATH);
            
            if (configContent == null || configContent.isEmpty()) {
                log.warn("Telegraf config not found or empty for target: {}/{}/{}", nsId, mciId, targetId);
                return List.of();
            }
            
            List<TelegrafPlugin> activePlugins = parseTelegrafConfig(configContent);
            
            Map<String, AgentPluginDefEntity> pluginDefMap = agentPluginDefService.getAllPluginDefinitions()
                .stream()
                .collect(Collectors.toMap(
                        AgentPluginDefEntity::getName,
                    entity -> entity
                ));

            List<MonitoringItemDTO> items = new ArrayList<>();
            int seq = 1;
            
            for (TelegrafPlugin plugin : activePlugins) {
                String key = plugin.getName() + "_" + plugin.getType();
                AgentPluginDefEntity pluginDef = pluginDefMap.get(key);
                
                MonitoringItemDTO item = MonitoringItemDTO.builder()
                    .seq((long) seq++)
                    .nsId(nsId)
                    .mciId(mciId)
                    .targetId(targetId)
                    .name(plugin.getName())
                    .state("ACTIVE")
                    .pluginSeq(pluginDef != null ? pluginDef.getSeq() : null)
                    .pluginName(plugin.getName())
                    .pluginType(plugin.getType())
                    .pluginConfig(plugin.getConfig())
                    .build();
                    
                items.add(item);
            }
            
            return items;
            
        } catch (Exception e) {
            log.error("Failed to read telegraf config for target: {}/{}/{}", nsId, mciId, targetId, e);
            return List.of();
        }
    }

    private List<TelegrafPlugin> parseTelegrafConfig(String configContent) {
        List<TelegrafPlugin> plugins = new ArrayList<>();
        String[] lines = configContent.split("\n");
        
        TelegrafPlugin currentPlugin = null;
        StringBuilder configBuilder = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            
            Matcher inputMatcher = INPUT_PATTERN.matcher(line);

            if (inputMatcher.find()) {
                if (currentPlugin != null) {
                    currentPlugin.setConfig(configBuilder.toString().trim());
                    plugins.add(currentPlugin);
                }
                currentPlugin = new TelegrafPlugin(inputMatcher.group(1), "INPUT");
                configBuilder = new StringBuilder(line + "\n");
            } else if (currentPlugin != null && !line.isEmpty()) {
                if (line.startsWith("[[")) {
                    currentPlugin.setConfig(configBuilder.toString().trim());
                    plugins.add(currentPlugin);
                    currentPlugin = null;
                    configBuilder = new StringBuilder();
                } else {
                    configBuilder.append(line).append("\n");
                }
            }
        }
        
        if (currentPlugin != null) {
            currentPlugin.setConfig(configBuilder.toString().trim());
            plugins.add(currentPlugin);
        }
        
        return plugins;
    }

    @Getter
    @Setter
    private static class TelegrafPlugin {
        private String name;
        private String type;
        private String config;

        public TelegrafPlugin(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
