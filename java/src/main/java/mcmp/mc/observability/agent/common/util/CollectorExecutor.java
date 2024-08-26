package mcmp.mc.observability.agent.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.monitoring.enums.StateOption;
import mcmp.mc.observability.agent.monitoring.enums.StateYN;
import mcmp.mc.observability.agent.monitoring.enums.TelegrafState;
import mcmp.mc.observability.agent.monitoring.loader.PluginLoader;
import mcmp.mc.observability.agent.monitoring.model.HostInfo;
import mcmp.mc.observability.agent.monitoring.model.HostItemInfo;
import mcmp.mc.observability.agent.monitoring.model.HostStorageInfo;
import mcmp.mc.observability.agent.monitoring.model.PluginDefInfo;
import mcmp.mc.observability.agent.monitoring.service.HostItemService;
import mcmp.mc.observability.agent.monitoring.service.HostService;
import mcmp.mc.observability.agent.monitoring.service.HostStorageService;
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
        sb.append(pluginDefInfo.getPluginId()).append("\n");
        if( pluginDefInfo.getIsInterval() ) sb.append("  interval = ").append("\"").append(hostItemInfo.getIntervalSec()).append("s\"\n");
        sb.append(hostItemInfo.getSetting());

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
        PluginDefInfo pluginDefInfo = pluginLoader.getPluginDefInfo(hostStorageInfo.getPluginSeq());
        StringBuilder sb = new StringBuilder("");
        sb.append(pluginDefInfo.getPluginId()).append("\n")
                .append(hostStorageInfo.getSetting());

        return sb.toString();
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
