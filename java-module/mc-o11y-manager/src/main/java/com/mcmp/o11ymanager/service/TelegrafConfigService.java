package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.entity.AgentPluginDefEntity;
import com.mcmp.o11ymanager.enums.ResponseCode;
import com.mcmp.o11ymanager.exception.TelegrafConfigException;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${deploy.site-code}")
    private String deploySiteCode;

    private final TumblebugService tumblebugService;
    private final AgentPluginDefService agentPluginDefService;
    
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\[\\[inputs\\.(\\w+)]]");

    private String getTelegrafConfigPath() {
        return "/cmp-agent/sites/" + deploySiteCode + "/telegraf/etc/telegraf.conf";
    }

    public List<MonitoringItemDTO> getTelegrafItems(String nsId, String mciId, String targetId, String userName) {
        try {
            if (!tumblebugService.isConnectedVM(nsId, mciId, targetId, userName)) {
                String errorMsg = String.format("VM not connected for target: %s/%s/%s", nsId, mciId, targetId);
                log.warn(errorMsg);
                throw new TelegrafConfigException(ResponseCode.VM_CONNECTION_FAILED, errorMsg);
            }

            String configContent = tumblebugService.executeCommand(nsId, mciId, targetId, userName, 
                "cat " + getTelegrafConfigPath());
            
            if (configContent == null || configContent.isEmpty()) {
                String errorMsg = String.format("Telegraf config not found for target: %s/%s/%s", nsId, mciId, targetId);
                log.warn(errorMsg);
                throw new TelegrafConfigException(ResponseCode.TELEGRAF_CONFIG_NOT_FOUND, errorMsg);
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
            
        } catch (TelegrafConfigException e) {
            throw e; // Re-throw custom exceptions
        } catch (Exception e) {
            String errorMsg = String.format("Failed to read telegraf config for target: %s/%s/%s", nsId, mciId, targetId);
            log.error(errorMsg, e);
            throw new TelegrafConfigException(ResponseCode.INTERNAL_SERVER_ERROR, errorMsg + ": " + e.getMessage());
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
