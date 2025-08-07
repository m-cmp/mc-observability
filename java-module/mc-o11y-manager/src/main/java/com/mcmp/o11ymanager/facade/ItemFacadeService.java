package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemRequestDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemUpdateDTO;
import com.mcmp.o11ymanager.dto.plugin.PluginDefDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.enums.ResponseCode;
import com.mcmp.o11ymanager.exception.TelegrafConfigException;
import com.mcmp.o11ymanager.service.interfaces.AgentPluginDefService;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemFacadeService {

    private final TelegrafFacadeService telegrafFacadeService;
    @Value("${deploy.site-code}")
    private String deploySiteCode;

    private final TumblebugService tumblebugService;
    private final AgentPluginDefService agentPluginDefService;
    
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\[\\[inputs\\.(\\w+)]]");

    private String getTelegrafConfigPath() {
        return "/cmp-agent/sites/" + deploySiteCode + "/telegraf/etc/telegraf.conf";
    }

    public void addTelegrafPlugin(String nsId, String mciId, String targetId, MonitoringItemRequestDTO dto) {
        try {
            TumblebugMCI.Vm vm = tumblebugService.getVm(nsId, mciId, targetId);
            if (vm == null) {
                String errorMsg = String.format("VM not found for target: %s/%s/%s", nsId, mciId, targetId);
                log.error(errorMsg);
                throw new TelegrafConfigException(ResponseCode.NOT_FOUND, errorMsg);
            }
            
            if (!tumblebugService.isConnectedVM(nsId, mciId, targetId)) {
                String errorMsg = String.format("VM not connected for target: %s/%s/%s", nsId, mciId, targetId);
                log.error(errorMsg);
                throw new TelegrafConfigException(ResponseCode.VM_CONNECTION_FAILED, errorMsg);
            }

            // Plugin definition에서 해당 plugin 찾기
            PluginDefDTO pluginDef = agentPluginDefService.getAllPluginDefinitions()
                .stream()
                .filter(def -> def.getSeq().equals(dto.getPluginSeq()))
                .findFirst()
                .orElseThrow(() -> new TelegrafConfigException(ResponseCode.NOT_FOUND, "Plugin definition not found"));


            // 기존 config 읽기
            String configContent = tumblebugService.executeCommand(nsId, mciId, targetId,
                "sudo cat " + getTelegrafConfigPath());
            
            if (configContent == null || configContent.isEmpty()) {
                String errorMsg = String.format("Telegraf config not found for target: %s/%s/%s", nsId, mciId, targetId);
                log.error(errorMsg);
                throw new TelegrafConfigException(ResponseCode.TELEGRAF_CONFIG_NOT_FOUND, errorMsg);
            }

            // 새로운 plugin 설정 추가
            String decodedConfig = new String(Base64.getDecoder().decode(dto.getPluginConfig()));
            String newPluginSection = pluginDef.getPluginId() + "\n" + decodedConfig + "\n\n";
            
            // config 파일에 추가
            String updatedConfig = configContent + "\n" + newPluginSection;
            
            // 파일 업데이트
            String updateCommand = String.format("echo '%s' | sudo tee %s > /dev/null",
                updatedConfig.replace("'", "'\"'\"'"), getTelegrafConfigPath());
            tumblebugService.executeCommand(nsId, mciId, targetId, updateCommand);

            telegrafFacadeService.restart(nsId, mciId, targetId);
            
        } catch (TelegrafConfigException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Failed to add telegraf plugin for target: %s/%s/%s", nsId, mciId, targetId);
            log.error(errorMsg, e);
            throw new TelegrafConfigException(ResponseCode.INTERNAL_SERVER_ERROR, errorMsg + ": " + e.getMessage());
        }
    }

    public void updateTelegrafPlugin(String nsId, String mciId, String targetId, MonitoringItemUpdateDTO dto) {
        try {
            TumblebugMCI.Vm vm = tumblebugService.getVm(nsId, mciId, targetId);
            if (vm == null) {
                String errorMsg = String.format("VM not found for target: %s/%s/%s", nsId, mciId, targetId);
                log.error(errorMsg);
                throw new TelegrafConfigException(ResponseCode.NOT_FOUND, errorMsg);
            }

            if (!tumblebugService.isConnectedVM(nsId, mciId, targetId)) {
                String errorMsg = String.format("VM not connected for target: %s/%s/%s", nsId, mciId, targetId);
                log.warn(errorMsg);
                throw new TelegrafConfigException(ResponseCode.VM_CONNECTION_FAILED, errorMsg);
            }

            // 기존 config 읽기 및 파싱
            String configContent = tumblebugService.executeCommand(nsId, mciId, targetId,
                "sudo cat " + getTelegrafConfigPath());
            
            if (configContent == null || configContent.isEmpty()) {
                String errorMsg = String.format("Telegraf config not found for target: %s/%s/%s", nsId, mciId, targetId);
                log.warn(errorMsg);
                throw new TelegrafConfigException(ResponseCode.TELEGRAF_CONFIG_NOT_FOUND, errorMsg);
            }

            // seq에 해당하는 plugin 찾아서 수정
            List<MonitoringItemDTO> currentItems = getTelegrafItems(nsId, mciId, targetId);
            MonitoringItemDTO targetItem = currentItems.stream()
                .filter(item -> item.getSeq().equals(dto.getSeq()))
                .findFirst()
                .orElseThrow(() -> new TelegrafConfigException(ResponseCode.NOT_FOUND, "Plugin not found in current config"));

            // config 파일에서 해당 plugin section 교체
            String decodedNewConfig = new String(Base64.getDecoder().decode(dto.getPluginConfig()));
            String updatedConfig = replacePluginInConfig(configContent, targetItem.getName(), decodedNewConfig);
            
            // 파일 업데이트
            String updateCommand = String.format("echo '%s' | sudo tee %s > /dev/null",
                updatedConfig.replace("'", "'\"'\"'"), getTelegrafConfigPath());
            tumblebugService.executeCommand(nsId, mciId, targetId, updateCommand);
            
        } catch (TelegrafConfigException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Failed to update telegraf plugin for target: %s/%s/%s", nsId, mciId, targetId);
            log.error(errorMsg, e);
            throw new TelegrafConfigException(ResponseCode.INTERNAL_SERVER_ERROR, errorMsg + ": " + e.getMessage());
        }
    }

    public void deleteTelegrafPlugin(String nsId, String mciId, String targetId, Long itemSeq) {
        try {
            TumblebugMCI.Vm vm = tumblebugService.getVm(nsId, mciId, targetId);
            if (vm == null) {
                String errorMsg = String.format("VM not found for target: %s/%s/%s", nsId, mciId, targetId);
                log.error(errorMsg);
                throw new TelegrafConfigException(ResponseCode.NOT_FOUND, errorMsg);
            }

            if (!tumblebugService.isConnectedVM(nsId, mciId, targetId)) {
                String errorMsg = String.format("VM not connected for target: %s/%s/%s", nsId, mciId, targetId);
                log.warn(errorMsg);
                throw new TelegrafConfigException(ResponseCode.VM_CONNECTION_FAILED, errorMsg);
            }

            // 기존 config 읽기
            String configContent = tumblebugService.executeCommand(nsId, mciId, targetId,
                "sudo cat " + getTelegrafConfigPath());
            
            if (configContent == null || configContent.isEmpty()) {
                String errorMsg = String.format("Telegraf config not found for target: %s/%s/%s", nsId, mciId, targetId);
                log.warn(errorMsg);
                throw new TelegrafConfigException(ResponseCode.TELEGRAF_CONFIG_NOT_FOUND, errorMsg);
            }

            // seq에 해당하는 plugin 찾기
            List<MonitoringItemDTO> currentItems = getTelegrafItems(nsId, mciId, targetId);
            MonitoringItemDTO targetItem = currentItems.stream()
                .filter(item -> item.getSeq().equals(itemSeq))
                .findFirst()
                .orElseThrow(() -> new TelegrafConfigException(ResponseCode.NOT_FOUND, "Plugin not found in current config"));

            // config 파일에서 해당 plugin section 제거
            String updatedConfig = removePluginFromConfig(configContent, targetItem.getName());
            
            // 파일 업데이트
            String updateCommand = String.format("echo '%s' | sudo tee %s > /dev/null",
                updatedConfig.replace("'", "'\"'\"'"), getTelegrafConfigPath());
            tumblebugService.executeCommand(nsId, mciId, targetId, updateCommand);

            telegrafFacadeService.restart(nsId, mciId, targetId);
            
        } catch (TelegrafConfigException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Failed to delete telegraf plugin for target: %s/%s/%s", nsId, mciId, targetId);
            log.error(errorMsg, e);
            throw new TelegrafConfigException(ResponseCode.INTERNAL_SERVER_ERROR, errorMsg + ": " + e.getMessage());
        }
    }


    private String toPluginId(String name, String type) {
        return "[[inputs." + name + "]]";
    }


    public List<MonitoringItemDTO> getTelegrafItems(String nsId, String mciId, String targetId) {
        try {

            if (!tumblebugService.isConnectedVM(nsId, mciId, targetId)) {
                String errorMsg = String.format("VM not connected for target: %s/%s/%s", nsId, mciId, targetId);
                log.warn(errorMsg);
                throw new TelegrafConfigException(ResponseCode.VM_CONNECTION_FAILED, errorMsg);
            }

            String configContent = tumblebugService.executeCommand(nsId, mciId, targetId,
                "sudo cat " + getTelegrafConfigPath());
            
            if (configContent == null || configContent.isEmpty()) {
                String errorMsg = String.format("Telegraf config not found for target: %s/%s/%s", nsId, mciId, targetId);
                log.warn(errorMsg);
                throw new TelegrafConfigException(ResponseCode.TELEGRAF_CONFIG_NOT_FOUND, errorMsg);
            }
            
            List<TelegrafPlugin> activePlugins = parseTelegrafConfig(configContent);



            Map<String, PluginDefDTO> pluginDefMap = agentPluginDefService.getAllPluginDefinitions()
                .stream()
                .collect(Collectors.toMap(
                    PluginDefDTO::getPluginId,
                    entity -> entity
                ));

            List<MonitoringItemDTO> items = new ArrayList<>();
            int seq = 1;

            for (TelegrafPlugin plugin : activePlugins) {
                String key =  toPluginId(plugin.getName(), plugin.getType());

                PluginDefDTO pluginDef = pluginDefMap.get(key);
                if (pluginDef == null) {
                    log.info("❗ pluginDef NOT FOUND for pluginId: " + key);
                }


               pluginDef = pluginDefMap.get(key);

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
                    String config = configBuilder.toString().trim();
                    String encodedConfig = Base64.getEncoder().encodeToString(config.getBytes());
                    currentPlugin.setConfig(encodedConfig);
                    plugins.add(currentPlugin);
                }
                currentPlugin = new TelegrafPlugin(inputMatcher.group(1), "INPUT");
                configBuilder = new StringBuilder();

            } else if (currentPlugin != null && !line.isEmpty()) {
                if (line.startsWith("[[")) {
                    String config = configBuilder.toString().trim();
                    String encodedConfig = Base64.getEncoder().encodeToString(config.getBytes());
                    currentPlugin.setConfig(encodedConfig);
                    plugins.add(currentPlugin);
                    currentPlugin = null;
                    configBuilder = new StringBuilder();
                } else {
                    configBuilder.append(line).append("\n");
                }
            }
        }

        if (currentPlugin != null) {
            String config = configBuilder.toString().trim();
            String encodedConfig = Base64.getEncoder().encodeToString(config.getBytes());
            currentPlugin.setConfig(encodedConfig);
            plugins.add(currentPlugin);
        }

        return plugins;
    }

    private String replacePluginInConfig(String configContent, String pluginName, String newConfig) {
        String[] lines = configContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inTargetPlugin = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            
            if (trimmedLine.matches("\\[\\[inputs\\." + pluginName + "]]")) {
                // 대상 plugin 시작
                inTargetPlugin = true;
                result.append("[[inputs.").append(pluginName).append("]]\n");
                result.append(newConfig).append("\n");
            } else if (inTargetPlugin && trimmedLine.startsWith("[[")) {
                // 다른 plugin 시작, 현재 plugin 끝
                inTargetPlugin = false;
                result.append(line).append("\n");
            } else if (!inTargetPlugin) {
                // 대상이 아닌 라인들은 그대로 유지
                result.append(line).append("\n");
            }
            // inTargetPlugin이면서 다른 [[로 시작하지 않는 라인들은 건너뜀 (기존 설정 제거)
        }
        
        return result.toString();
    }

    private String removePluginFromConfig(String configContent, String pluginName) {
        String[] lines = configContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inTargetPlugin = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            if (trimmedLine.matches("\\[\\[inputs\\." + pluginName + "]]")) {
                // 대상 plugin 시작, 이 섹션 전체를 건너뜀
                inTargetPlugin = true;
            } else if (inTargetPlugin && trimmedLine.startsWith("[[")) {
                // 다른 plugin 시작, 현재 plugin 끝
                inTargetPlugin = false;
                result.append(line).append("\n");
            } else if (!inTargetPlugin) {
                // 대상이 아닌 라인들은 그대로 유지
                result.append(line).append("\n");
            }
            // inTargetPlugin이면서 다른 [[로 시작하지 않는 라인들은 건너뜀 (해당 plugin 제거)
        }
        
        return result.toString();
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
