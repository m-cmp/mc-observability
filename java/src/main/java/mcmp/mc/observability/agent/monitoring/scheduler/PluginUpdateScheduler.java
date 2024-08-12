package mcmp.mc.observability.agent.monitoring.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.monitoring.loader.PluginLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginUpdateScheduler {
    private final PluginLoader pluginLoader;

    @Scheduled(cron = "${scheduler.expression.plugin-update:*/30 * * * * ?}")
    public void checkConfigUpdate() {
        try {
            pluginLoader.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
