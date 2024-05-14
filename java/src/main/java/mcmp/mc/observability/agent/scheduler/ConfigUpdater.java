package mcmp.mc.observability.agent.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.config.ApiProfileCondition;
import mcmp.mc.observability.agent.enums.StateOption;
import mcmp.mc.observability.agent.model.HostInfo;
import mcmp.mc.observability.agent.model.HostItemInfo;
import mcmp.mc.observability.agent.model.HostStorageInfo;
import mcmp.mc.observability.agent.service.HostItemService;
import mcmp.mc.observability.agent.service.HostService;
import mcmp.mc.observability.agent.service.HostStorageService;
import mcmp.mc.observability.agent.util.CollectorExecutor;
import mcmp.mc.observability.agent.util.GlobalProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info("Config Polling end");
    }
}
