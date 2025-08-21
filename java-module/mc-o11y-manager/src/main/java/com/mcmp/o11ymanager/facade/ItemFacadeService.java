package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemRequestDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemUpdateDTO;
import com.mcmp.o11ymanager.dto.plugin.PluginDefDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.enums.ResponseCode;
import com.mcmp.o11ymanager.exception.TelegrafConfigException;
import com.mcmp.o11ymanager.service.interfaces.AgentPluginDefService;
import com.mcmp.o11ymanager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemFacadeService {

    private final TelegrafFacadeService telegrafFacadeService;
    @Value("${deploy.site-code}")
    private String deploySiteCode;

    private final TumblebugService tumblebugService;
    private final AgentPluginDefService agentPluginDefService;
    private final InfluxDbService influxDbService;
    private final TargetService targetService;
    
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\[\\[inputs\\.(\\w+)]]");

    private static final Pattern OUTPUT_PATTERN = Pattern.compile("\\[\\[outputs\\.(\\w+)]]");

    // ------------------------------------update output--------------------------------------------------//


    @Transactional
    public void updateInfluxOutputForAllTargets(String nsId, String mciId, InfluxDTO req) {
        // 0) 연결성 사전 검증
        if (!influxDbService.isConnectedDb(req)) {
            throw new TelegrafConfigException(ResponseCode.BAD_REQUEST,
                "Invalid Influx connection: " + req.getUrl());
        }

        // 1) 대상 타깃 수집
        var targetIds = targetService.getTargetIds(nsId, mciId);
        if (targetIds.isEmpty()) {
            log.warn("[influx-output] no targets for ns={}, mci={}", nsId, mciId);
            return;
        }

        // 2) 대표 influxDbId 결정: 현 ns/mci에서 사용 중인 id 집합을 보고 1개 선택
        var ids = targetService.getDistinctInfluxIds(nsId, mciId);
        if (ids.isEmpty()) {
            throw new TelegrafConfigException(ResponseCode.NOT_FOUND,
                "No influx mapping under ns/mci: " + nsId + "/" + mciId);
        }
        Long influxDbId = ids.get(0);           // 정책: 여러 개면 첫 번째 사용(필요 시 검증/선택 로직 추가)
        // if (ids.size() > 1) log.warn("multiple influx ids found {}, picking {}", ids, influxDbId);

        // 3) InfluxEntity를 요청 DTO대로 업데이트(영속화)
        influxDbService.updateInflux(influxDbId, req);

        // 4) ns/mci 하위 모든 Target을 동일 influx로 일괄 재바인딩(FK + seq= id)
        targetService.rebindTargetsToInflux(nsId, mciId, influxDbId);

        // 5) telegraf.conf의 [[outputs.influxdb]] 바디 교체 + 재시작
        String path = getTelegrafConfigPath();
        String newBody = buildInfluxV1OutputBody(req);

        for (String targetId : targetIds) {
            try {
                if (!tumblebugService.isConnectedVM(nsId, mciId, targetId)) {
                    log.warn("[influx-output] VM not connected {}/{}/{}", nsId, mciId, targetId);
                    continue;
                }

                String content = tumblebugService.executeCommand(nsId, mciId, targetId, "sudo cat " + path);
                if (content == null || content.isEmpty()) {
                    log.warn("[influx-output] telegraf.conf not found {}/{}/{}", nsId, mciId, targetId);
                    continue;
                }

                if (!hasOutputsInflux(content)) {
                    log.warn("[influx-output] [[outputs.influxdb]] not found {}/{}/{}", nsId, mciId, targetId);
                    continue;
                }

                String updated = replaceOutputInConfig(content, "influxdb", newBody);
                String cmd = String.format("echo '%s' | sudo tee %s > /dev/null",
                    updated.replace("'", "'\"'\"'"), path);
                tumblebugService.executeCommand(nsId, mciId, targetId, cmd);

                telegrafFacadeService.restart(nsId, mciId, targetId);
                log.info("[influx-output] applied & restarted: {}/{}/{}", nsId, mciId, targetId);

            } catch (Exception e) {
                log.warn("[influx-output] apply failed {}/{}/{} err={}",
                    nsId, mciId, targetId, e.toString());
            }
        }
    }

    // === helpers ===
    private boolean hasOutputsInflux(String content) {
        var m = OUTPUT_PATTERN.matcher(content);
        while (m.find()) if ("influxdb".equals(m.group(1))) return true;
        return false;
    }

    private String buildInfluxV1OutputBody(InfluxDTO inf) {
        return """
    urls = ["%s"]
    database = "%s"
    username = "%s"
    password = "%s"
    """.formatted(inf.getUrl(), inf.getDatabase(), inf.getUsername(), inf.getPassword());
    }

    private String replaceOutputInConfig(String configContent, String pluginName, String newConfig) {
        String[] lines = configContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inTarget = false;

        for (String line : lines) {
            String t = line.trim();

            if (t.matches("\\[\\[outputs\\." + pluginName + "]]")) {
                inTarget = true;
                result.append("[[outputs.").append(pluginName).append("]]\n");
                result.append(newConfig).append("\n");
            } else if (inTarget && t.startsWith("[[")) {
                inTarget = false;
                result.append(line).append("\n");
            } else if (!inTarget) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }


    // ------------------------------------finish influx--------------------------------------------------//





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
