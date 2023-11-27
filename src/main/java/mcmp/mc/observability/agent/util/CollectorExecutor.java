package mcmp.mc.observability.agent.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.enums.StateOption;
import mcmp.mc.observability.agent.enums.StateYN;
import mcmp.mc.observability.agent.enums.TelegrafState;
import mcmp.mc.observability.agent.loader.PluginLoader;
import mcmp.mc.observability.agent.model.HostInfo;
import mcmp.mc.observability.agent.model.HostItemInfo;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import mcmp.mc.observability.agent.model.PluginDefInfo;
import mcmp.mc.observability.agent.service.HostItemService;
import mcmp.mc.observability.agent.service.HostService;
import mcmp.mc.observability.agent.service.HostStorageService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorExecutor {

    private final HostService hostService;
    private final HostItemService hostItemService;
    private final HostStorageService hostStorageService;
    private final PluginLoader pluginLoader;
    private final GlobalProperties globalProperties;
    private Process AGENT_PROCESS = null;
    private final ClassPathResource globalConfigResource = new ClassPathResource("defaultGlobalConfig");
    private final ClassPathResource influxDBV1ConfigResource = new ClassPathResource("InfluxDBV1Config");
    private String defaultInfluxDBV1Config;

    @PostConstruct
    private void setDefaultInfluxDBV1Config() {
        if(!influxDBV1ConfigResource.exists()){
            log.error("Invalid filePath : InfluxDBV1Config");
            throw new IllegalArgumentException();
        }

        log.info("file path exists = {}", influxDBV1ConfigResource.exists());

        try (InputStream is = new BufferedInputStream(influxDBV1ConfigResource.getInputStream())) {
            defaultInfluxDBV1Config = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void stopAgent() {
        if( AGENT_PROCESS == null ) return;
        AGENT_PROCESS.destroy();

        String uuid = globalProperties.getUuid();
        Long hostSeq = hostService.getHostSeq(uuid);
        hostService.updateTelegrafState(hostSeq, TelegrafState.STOPPED);
    }

    public void startAgent() {
        try {
            TelegrafState state = TelegrafState.RUNNING;
            if( AGENT_PROCESS == null || !AGENT_PROCESS.isAlive() ) {
                Process once = new ProcessBuilder().command(Constants.COLLECTOR_PATH, "--config", Constants.COLLECTOR_CONFIG_PATH, "--config-directory", Constants.COLLECTOR_CONFIG_DIR_PATH, "--watch-config", "poll", "--once").start();
                once.waitFor();

                if( once.exitValue() != 0) {
                    state = TelegrafState.FAILED;
                }else {
                    AGENT_PROCESS = new ProcessBuilder().command(Constants.COLLECTOR_PATH, "--config", Constants.COLLECTOR_CONFIG_PATH, "--config-directory", Constants.COLLECTOR_CONFIG_DIR_PATH, "--watch-config", "poll").start();
                }
            }

            String uuid = globalProperties.getUuid();
            Long hostSeq = hostService.getHostSeq(uuid);

            hostService.updateTelegrafState(hostSeq, state);
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void monitoringHostYN(Long hostSeq) {
        HostInfo hostInfo = hostService.getDetail(hostSeq);

        if (hostInfo.getMonitoringYn() == StateYN.Y) {
            startAgent();
        } else {
            stopAgent();
        }
    }

    public TelegrafState getTelegrafState() {
        if(AGENT_PROCESS != null) {
            if(!AGENT_PROCESS.isAlive()) {
                if (AGENT_PROCESS.exitValue() != 0) {
                    return TelegrafState.FAILED;
                } else {
                    return TelegrafState.STOPPED;
                }
            }
            return TelegrafState.RUNNING;
        }
        return TelegrafState.STOPPED;
    }

    public boolean isInactiveAgent() {
        return AGENT_PROCESS == null;
    }

    public String globalTelegrafConfig(String receiverId) {
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

        return sb.toString().replaceAll("@UUID", receiverId);
    }

    public void syncAction(HostInfo hostInfo, List<HostItemInfo> hostItemInfoList, List<HostStorageInfo> hostStorageInfoList) {
        try {

            manageHostItem(hostItemInfoList);
            manageHostStorage(hostStorageInfoList);

            if( hostInfo.getSyncYN() == StateYN.N ) {
                hostService.updateHost(HostInfo.builder().seq(hostInfo.getSeq()).syncYN(StateYN.Y).build());
            }

            rewriteTelegrafConf();

            if( hostInfo.getMonitoringYn() == StateYN.Y && isInactiveAgent() ) {
                startAgent();
            } else if( hostInfo.getMonitoringYn() == StateYN.N && !isInactiveAgent()) {
                stopAgent();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void manageHostItem(List<HostItemInfo> hostItemInfoList) {
        try {
            if (hostItemInfoList == null || hostItemInfoList.isEmpty() ) return ;

            for (HostItemInfo hostItemInfo : hostItemInfoList) {
                String filePath = Constants.COLLECTOR_CONFIG_DIR_PATH + "/i_" + hostItemInfo.getSeq() + ".conf";

                if( hostItemInfo.getState() == StateOption.DELETE ) {
                    Utils.deleteFile(filePath);
                    hostItemService.deleteItemRow(hostItemInfo.getSeq());
                }
                else {
                    if( hostItemInfo.getMonitoringYn() == StateYN.N ) {
                        Utils.deleteFile(filePath);
                    }
                    else {
                        Utils.writeFile(makeItemConfig(hostItemInfo), filePath);
                    }
                    hostItemService.updateItemConf(hostItemInfo.getSeq());
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private String makeItemConfig(HostItemInfo hostItemInfo) {
        PluginDefInfo pluginDefInfo = pluginLoader.getPluginDefInfo(hostItemInfo.getPluginSeq());
        StringBuilder sb = new StringBuilder("");
        sb.append(pluginDefInfo.getPluginId()).append("\n")
                .append("  interval = ").append("\"").append(hostItemInfo.getIntervalSec()).append("s\"\n")
                .append(hostItemInfo.getSetting());

        return sb.toString();
    }

    public void manageHostStorage(List<HostStorageInfo> list) {

        try {
            if( list == null || list.isEmpty() ) return ;

            for( HostStorageInfo info : list ) {
                String filePath = Constants.COLLECTOR_CONFIG_DIR_PATH + "/s_" + info.getSeq() + ".conf";

                if( info.getState() == StateOption.DELETE ) {
                    Utils.deleteFile(filePath);
                    hostStorageService.deleteStorageRow(info.getSeq());
                }
                else {
                    if( info.getMonitoringYn() == StateYN.N ) {
                        Utils.deleteFile(filePath);
                    }
                    else {
                        Utils.writeFile(makeStorageConfig(info), filePath);
                    }
                    hostStorageService.updateStorageConf(info.getSeq());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private String makeStorageConfig(HostStorageInfo hostStorageInfo) {
        JsonObject info = new Gson().fromJson(hostStorageInfo.getInfo(), JsonObject.class);
        return defaultInfluxDBV1Config
                .replaceAll("@IS_URL",              (info.get("url") == null? "#": ""))
                .replaceAll("@IS_DATABASE",         (info.get("database") == null? "#": ""))
                .replaceAll("@IS_RETENTION_POLICY", (info.get("retentionPolicy") == null? "#": ""))
                .replaceAll("@IS_USERNAME",         (info.get("username") == null? "#": ""))
                .replaceAll("@IS_PASSWORD",         (info.get("password") == null? "#": ""))
                .replaceAll("@URL",              (info.get("url") == null? "":             info.get("url").getAsString()))
                .replaceAll("@DATABASE",         (info.get("database") == null? "":        info.get("database").getAsString()))
                .replaceAll("@RETENTION_POLICY", (info.get("retentionPolicy") == null? "": info.get("retentionPolicy").getAsString()))
                .replaceAll("@USERNAME",         (info.get("username") == null? "":        info.get("username").getAsString()))
                .replaceAll("@PASSWORD",         (info.get("password") == null? "":        info.get("password").getAsString()))
                ;
    }

    private void rewriteTelegrafConf() {
        try {
            String content = Utils.readFile(Constants.COLLECTOR_CONFIG_PATH);
            Utils.writeFile(content, Constants.COLLECTOR_CONFIG_PATH);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
