package mcmp.mc.observability.mco11yagent.monitoring.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.enums.OS;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.PluginMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.MonitoringConfigInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.PluginDefInfo;
import mcmp.mc.observability.mco11yagent.monitoring.service.MonitoringConfigService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static mcmp.mc.observability.mco11yagent.monitoring.common.Constants.COLLECTOR_CONFIG_DIR_PATH;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorExecutor {
    private Process COLLECTOR_PROCESS = null;
    private final ClassPathResource globalConfigResource = new ClassPathResource("defaultGlobalConfig");

    private final PluginMapper pluginMapper;
    private final MonitoringConfigService monitoringConfigService;

    @PreDestroy
    public void stopCollector() {
        if( COLLECTOR_PROCESS == null ) return;
        COLLECTOR_PROCESS.destroy();
    }

    public void startCollector() {
        try {
            if (COLLECTOR_PROCESS == null || !COLLECTOR_PROCESS.isAlive()) {
                Process onceProcess = new ProcessBuilder().command(Constants.COLLECTOR_PATH, "--config", Constants.COLLECTOR_CONFIG_PATH, "--config-directory", Constants.COLLECTOR_CONFIG_DIR_PATH, "--watch-config", "poll", "--once").start();
                onceProcess.waitFor();

                if( onceProcess.exitValue() == 0 ) {
                    COLLECTOR_PROCESS = new ProcessBuilder().command(Constants.COLLECTOR_PATH, "--config", Constants.COLLECTOR_CONFIG_PATH, "--config-directory", Constants.COLLECTOR_CONFIG_DIR_PATH, "--watch-config", "poll").start();
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error(ExceptionUtils.getMessage(e));
        }
    }

    public String globalTelegrafConfig() {
        StringBuilder sb = new StringBuilder();

        if(!globalConfigResource.exists()){
            log.error("Invalid filePath : defaultGlobalConfig");
            throw new IllegalArgumentException();
        }

        log.info("file path exists = {}", globalConfigResource.exists());

        try (InputStream is = new BufferedInputStream(globalConfigResource.getInputStream())) {
            String text = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            sb.append(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String nsId = System.getProperty(Constants.PROPERTY_NS_ID);
        if( nsId == null ) nsId = "";
        String targetId = System.getProperty(Constants.PROPERTY_TARGET_ID);
        if( targetId == null ) targetId = "";
        return sb.toString().replaceAll("@NS_ID", nsId).replaceAll("@TARGET_ID", targetId);
    }

    public boolean isCollectorAlive() {
        return COLLECTOR_PROCESS != null && COLLECTOR_PROCESS.isAlive();
    }

    public void updateConfigFile() {
        String nsId = System.getProperty(Constants.PROPERTY_NS_ID);
        if( nsId == null ) nsId = "";
        String targetId = System.getProperty(Constants.PROPERTY_TARGET_ID);
        if( targetId == null ) targetId = "";

        List<MonitoringConfigInfo> configList = monitoringConfigService.list(nsId, targetId);
        Map<Long, PluginDefInfo> pluginMap = pluginMapper.getAllPlugin();

        boolean isUpdate = configList.stream().anyMatch(f -> !f.getState().equalsIgnoreCase("NONE"));

        configList.forEach(f -> {
            StringBuilder sb = new StringBuilder();
            String filePath = Constants.COLLECTOR_CONFIG_DIR_PATH + "/c_" + f.getSeq() + ".conf";
            try {
                switch (f.getState()) {
                    case "ADD", "UPDATE" -> {
                        sb.append(pluginMap.get(f.getPluginSeq()).getPluginId()).append("\n").append(f.getPluginConfig());
                        Utils.writeFile(sb.toString(), filePath);
                        monitoringConfigService.updateState(f, "NONE");
                    }
                    case "DELETE" -> {
                        Utils.deleteFile(filePath);
                        monitoringConfigService.delete(f.getNsId(), f.getTargetId(), f.getSeq());
                    }
                }
            } catch (IOException e) {
                log.error(ExceptionUtils.getMessage(e));
            }
        });

        // check file count vs config count reupdate

        if(isUpdate) {
            rewriteTelegrafConf();
        }
    }

    private void rewriteTelegrafConf() {
        try {
            String content = Utils.readFile(Constants.COLLECTOR_CONFIG_PATH);
            Utils.writeFile(content, Constants.COLLECTOR_CONFIG_PATH);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private int getConfFileCount() throws IOException, InterruptedException {
        String command = "";
        String result = "";
        switch (OS.parseProperty()) {
            case WINDOWS -> {
                command = "Get-ChildItem -Path " + COLLECTOR_CONFIG_DIR_PATH + " -Filter *.conf | Measure-Object | Select-Object -ExpandProperty Count";
                result = Utils.runExec(new String[]{"powershell", "/c", command});
            }
            case LINUX, UNIX -> {
                command = "find " + COLLECTOR_CONFIG_DIR_PATH + " -type f -name \"*.conf\" | wc -l";
                result = Utils.runExec(new String[]{"/bin/sh", "-c", command});
            }
        }

        return Integer.parseInt(result.trim());
    }
}
