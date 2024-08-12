package mcmp.mc.observability.agent.monitoring.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.config.ApiProfileCondition;
import mcmp.mc.observability.agent.monitoring.enums.OS;
import mcmp.mc.observability.agent.monitoring.enums.StateOption;
import mcmp.mc.observability.agent.monitoring.model.HostInfo;
import mcmp.mc.observability.agent.monitoring.model.HostItemInfo;
import mcmp.mc.observability.agent.monitoring.model.HostStorageInfo;
import mcmp.mc.observability.agent.monitoring.service.HostItemService;
import mcmp.mc.observability.agent.monitoring.service.HostService;
import mcmp.mc.observability.agent.monitoring.service.HostStorageService;
import mcmp.mc.observability.agent.common.util.CollectorExecutor;
import mcmp.mc.observability.agent.common.util.GlobalProperties;
import mcmp.mc.observability.agent.common.util.Output;
import mcmp.mc.observability.agent.common.util.Utils;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mcmp.mc.observability.agent.common.Constants.COLLECTOR_CONFIG_DIR_PATH;

@Slf4j
@Service
@Conditional(ApiProfileCondition.class)
@RequiredArgsConstructor
public class ConfigUpdater {
    private final CollectorExecutor collectorExecutor;
    private final HostService hostService;
    private final HostItemService hostItemService;
    private final HostStorageService hostStorageService;
    private final GlobalProperties globalProperties;

    @Scheduled(cron = "${scheduler.expression.config-check:*/30 * * * * ?}")
    public void checkConfigUpdate() {
        try {
            Long hostSeq = hostService.getHostSeq(globalProperties.getUuid());
            collectorExecutor.monitoringHostYN(hostSeq);

            HostInfo hostInfo = hostService.getDetail(hostSeq);

            Map<String, Object> params = new HashMap<>();
            params.put("notState", StateOption.NONE.name());
            params.put("hostSeq", hostSeq);

            List<HostItemInfo> hostItemInfoList = hostItemService.getList(params);
            List<HostStorageInfo> hostStorageInfoList = hostStorageService.getList(params);

            if( hostItemInfoList.size() > 0 || hostStorageInfoList.size() > 0 ) {
                collectorExecutor.syncAction(hostInfo, hostItemInfoList, hostStorageInfoList);
            }

            List<HostItemInfo> syncedItemInfoList = hostItemService.getList(params);
            List<HostStorageInfo> syncedStorageInfoList = hostStorageService.getList(params);
            int confFileCount = getConfFileCount();
            if (confFileCount != syncedItemInfoList.size() + syncedStorageInfoList.size()) {
                collectorExecutor.syncAction(hostInfo, hostItemInfoList, hostStorageInfoList);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info("Config Polling end");
    }

    private int getConfFileCount() throws IOException, InterruptedException {
        Output output = new Output();
        String command = "";
        switch (OS.parseProperty()) {
            case WINDOWS:
                //  Get-ChildItem -Path filePath -Filter *.conf | Measure-Object | Select-Object -ExpandProperty Count
                command = "Get-ChildItem -Path " + COLLECTOR_CONFIG_DIR_PATH + " -Filter *.conf | Measure-Object | Select-Object -ExpandProperty Count";
                Utils.runCommand(new String[]{"powershell", "/c", command}, output);
                break;
            case LINUX:
            case UNIX:
                //  find /home/files/Argos-agent/conf -type f -name "*.conf" | wc -l
                command = "find " + COLLECTOR_CONFIG_DIR_PATH + " -type f -name \"*.conf\" | wc -l";
                Utils.runCommand(new String[]{"/bin/sh", "-c", command}, output);
                break;
            default:
                return 0;
        }

        return Integer.parseInt(output.getText().trim());
    }
}
