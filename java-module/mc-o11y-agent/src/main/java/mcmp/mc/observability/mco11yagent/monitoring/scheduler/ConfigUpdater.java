package mcmp.mc.observability.mco11yagent.monitoring.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.util.CollectorExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigUpdater {

    private final CollectorExecutor collectorExecutor;

    @Scheduled(cron = "*/30 * * * * ?")
    public void run() {
        collectorExecutor.updateConfigFile();

        if( !collectorExecutor.isCollectorAlive() ) collectorExecutor.startCollector();
    }
}
