package mcmp.mc.observability.mco11yagent.monitoring.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.util.CollectorExecutor;
import mcmp.mc.observability.mco11yagent.monitoring.util.SpiderCollectorExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpiderMonitoringCollector {

    private final SpiderCollectorExecutor spiderMonitoringCollectorExecutor;

    @Scheduled(cron = "0 */1 * * * ?")
    public void run() {
        if( !spiderMonitoringCollectorExecutor.isSpiderCollectorAlive() ) spiderMonitoringCollectorExecutor.startSpiderCollector();
    }
}
