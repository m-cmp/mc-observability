package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentAction;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.semaphore.Project;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.port.SemaphorePort;
import com.mcmp.o11ymanager.manager.service.interfaces.TargetService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SchedulerFacadeService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @PostConstruct
    void debugSchedulerBean() {
        log.info(
                "==================================================[SchedulerFacadeService] injected scheduler bean={}, id={}==================================================",
                scheduler.getClass().getName(),
                System.identityHashCode(scheduler));

        scheduler.schedule(
                () ->
                        log.info(
                                "==================================================[SchedulerFacadeService] scheduler thread={}==================================================",
                                Thread.currentThread().getName()),
                0,
                TimeUnit.SECONDS);
    }

    private final SemaphorePort semaphorePort;
    private final TargetService targetService;

    @Value("${feign.semaphore.project-name}")
    private String projectName;

    @Value("${feign.semaphore.task-check-scheduler.check-interval-sec:5}")
    private int checkIntervalSec;

    @Value("${feign.semaphore.task-check-scheduler.max-wait-minutes:30}")
    private int maxWaitMinutes;

    public void scheduleTaskStatusCheck(
            String requestId,
            Integer taskId,
            String nsId,
            String mciId,
            String targetId,
            SemaphoreInstallMethod method,
            Agent agent) {
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        ScheduledFuture<?> future =
                scheduler.scheduleAtFixedRate(
                        () -> {
                            try {
                                long currentTime = System.currentTimeMillis();

                                AgentAction action;
                                Project project = semaphorePort.getProjectByName(projectName);
                                Task currentTask = semaphorePort.getTask(project.getId(), taskId);

                                log.info(
                                        "🔨🔨 --------------------Task Status-------------------- 🔨🔨");
                                log.debug(
                                        "Task Status - Request ID: {}, Target: {}/{}/{}, Agent: {}, Install Method: {}, Task ID: {}, Status: {}",
                                        requestId,
                                        nsId,
                                        mciId,
                                        targetId,
                                        agent,
                                        method,
                                        currentTask.getId(),
                                        currentTask.getStatus());

                                if ("waiting".equals(currentTask.getStatus())) {
                                    startTime.set(System.currentTimeMillis());
                                    return;
                                }

                                // 타임아웃의 경우
                                if (currentTime - startTime.get()
                                        > TimeUnit.MINUTES.toMillis(maxWaitMinutes)) {
                                    log.warn(
                                            "Task timed out after {} minutes. Resetting status to IDLE. Target: {}/{}/{}, Agent: {}",
                                            maxWaitMinutes,
                                            nsId,
                                            mciId,
                                            targetId,
                                            agent);

                                    if (Objects.requireNonNull(agent) == Agent.TELEGRAF) {
                                        targetService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        targetService.updateLogAgentTaskStatus(
                                                nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);
                                    }

                                    throw new TimeoutException(
                                            "Task max wait timed out after "
                                                    + maxWaitMinutes
                                                    + " minutes");
                                }

                                // 성공시
                                if ("success".equals(currentTask.getStatus())) {
                                    action = getAgentActionFinished(method, agent);
                                    log.debug(action.toString());
                                    log.debug("Task successful");
                                } else if ("error".equals(currentTask.getStatus())) {
                                    action = getAgentActionFailed(method, agent);
                                    log.debug("Task failed");
                                } else {
                                    return;
                                }

                                if (action != null) {
                                    log.debug(
                                            "Updating Agent History - Request ID: {}, Target: {}/{}/{}, Agent: {}, Action: {}",
                                            requestId,
                                            nsId,
                                            mciId,
                                            targetId,
                                            agent,
                                            action);
                                    if (agent == Agent.TELEGRAF) {
                                        targetService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        targetService.updateLogAgentTaskStatus(
                                                nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);
                                    }

                                    log.info(
                                            "🔨🔨 --------------------task end-------------------- 🔨🔨");
                                }

                                ScheduledFuture<?> scheduledFuture = futureRef.get();
                                if (scheduledFuture != null) {
                                    scheduledFuture.cancel(false);
                                }
                            } catch (Exception e) {
                                log.error("Error while checking task status: {}", e.getMessage());
                                ScheduledFuture<?> scheduledFuture = futureRef.get();
                                if (scheduledFuture != null) {
                                    scheduledFuture.cancel(false);
                                }

                                AgentAction action = getAgentActionFailed(method, agent);
                                if (action != null) {
                                    log.debug(
                                            "Updating Agent History - Request ID: {}, Target: {}/{}/{}, Agent: {}, Action: {}",
                                            requestId,
                                            nsId,
                                            mciId,
                                            targetId,
                                            agent,
                                            action);

                                    if (agent == Agent.TELEGRAF) {
                                        targetService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        targetService.updateLogAgentTaskStatus(
                                                nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);
                                    }
                                }
                            }
                        },
                        0,
                        checkIntervalSec,
                        TimeUnit.SECONDS);

        futureRef.set(future);
    }

    private AgentAction getAgentActionFinished(SemaphoreInstallMethod method, Agent agent) {
        AgentAction action = null;

        if (agent.equals(Agent.TELEGRAF)) {
            if (method == SemaphoreInstallMethod.INSTALL) {
                action = AgentAction.MONITORING_AGENT_INSTALL_FINISHED;
            } else if (method == SemaphoreInstallMethod.UPDATE) {
                action = AgentAction.MONITORING_AGENT_UPDATE_FINISHED;
            } else if (method == SemaphoreInstallMethod.UNINSTALL) {
                action = AgentAction.MONITORING_AGENT_UNINSTALL_FINISHED;
            } else if (method == SemaphoreInstallMethod.CONFIG_UPDATE) {
                action = AgentAction.MONITORING_AGENT_CONFIG_UPDATE_FINISHED;
            } else if (method == SemaphoreInstallMethod.ROLLBACK_CONFIG) {
                action = AgentAction.MONITORING_AGENT_CONFIG_ROLLBACK_FINISHED;
            }
        } else if (agent.equals(Agent.FLUENT_BIT)) {
            if (method == SemaphoreInstallMethod.INSTALL) {
                action = AgentAction.LOG_AGENT_INSTALL_FINISHED;
            } else if (method == SemaphoreInstallMethod.UPDATE) {
                action = AgentAction.LOG_AGENT_CONFIG_UPDATE_FINISHED;
            } else if (method == SemaphoreInstallMethod.UNINSTALL) {
                action = AgentAction.LOG_AGENT_UNINSTALL_FINISHED;
            } else if (method == SemaphoreInstallMethod.CONFIG_UPDATE) {
                action = AgentAction.LOG_AGENT_CONFIG_UPDATE_FINISHED;
            } else if (method == SemaphoreInstallMethod.ROLLBACK_CONFIG) {
                action = AgentAction.LOG_AGENT_CONFIG_ROLLBACK_FINISHED;
            }
        }

        return action;
    }

    private AgentAction getAgentActionFailed(SemaphoreInstallMethod method, Agent agent) {
        AgentAction action = null;

        if (agent.equals(Agent.TELEGRAF)) {
            if (method == SemaphoreInstallMethod.INSTALL) {
                action = AgentAction.MONITORING_AGENT_INSTALL_FAILED;
            } else if (method == SemaphoreInstallMethod.UPDATE) {
                action = AgentAction.MONITORING_AGENT_UPDATE_FAILED;
            } else if (method == SemaphoreInstallMethod.UNINSTALL) {
                action = AgentAction.MONITORING_AGENT_UNINSTALL_FAILED;
            } else if (method == SemaphoreInstallMethod.CONFIG_UPDATE) {
                action = AgentAction.MONITORING_AGENT_CONFIG_UPDATE_FAILED;
            } else if (method == SemaphoreInstallMethod.ROLLBACK_CONFIG) {
                action = AgentAction.MONITORING_AGENT_CONFIG_ROLLBACK_FAILED;
            }
        } else if (agent.equals(Agent.FLUENT_BIT)) {
            if (method == SemaphoreInstallMethod.INSTALL) {
                action = AgentAction.LOG_AGENT_INSTALL_FAILED;
            } else if (method == SemaphoreInstallMethod.UPDATE) {
                action = AgentAction.LOG_AGENT_CONFIG_UPDATE_FAILED;
            } else if (method == SemaphoreInstallMethod.UNINSTALL) {
                action = AgentAction.LOG_AGENT_UNINSTALL_FAILED;
            } else if (method == SemaphoreInstallMethod.CONFIG_UPDATE) {
                action = AgentAction.LOG_AGENT_CONFIG_UPDATE_FAILED;
            } else if (method == SemaphoreInstallMethod.ROLLBACK_CONFIG) {
                action = AgentAction.LOG_AGENT_CONFIG_ROLLBACK_FAILED;
            }
        }

        return action;
    }
}
