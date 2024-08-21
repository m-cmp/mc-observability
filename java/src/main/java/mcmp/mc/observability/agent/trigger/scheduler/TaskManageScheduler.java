package mcmp.mc.observability.agent.trigger.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.trigger.service.TriggerTaskManageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskManageScheduler {

    private final TriggerTaskManageService triggerTaskManageService;

    @Scheduled(cron = "${scheduler.expression.task-manage:0 */5 * * * ?}")
    public void manageTask() {
        try {
            triggerTaskManageService.manageTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
