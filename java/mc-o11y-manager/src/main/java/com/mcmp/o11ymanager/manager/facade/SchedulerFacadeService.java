package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentAction;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.semaphore.Project;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.port.SemaphorePort;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
    private final VMService vmService;

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
            String vmId,
            SemaphoreInstallMethod method,
            Agent agent) {

        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        ScheduledFuture<?> future =
                scheduler.scheduleAtFixedRate(
                        () -> {
                            try {
                                long currentTime = System.currentTimeMillis();

                                Project project = semaphorePort.getProjectByName(projectName);
                                Task currentTask = semaphorePort.getTask(project.getId(), taskId);
                                String status =
                                        Optional.ofNullable(currentTask.getStatus())
                                                .orElse("")
                                                .toLowerCase();

                                log.debug(
                                        "Task Status - Request ID: {}, VM: {}/{}/{}, Agent: {}, Method: {}, Task ID: {}, Status: {}",
                                        requestId,
                                        nsId,
                                        mciId,
                                        vmId,
                                        agent,
                                        method,
                                        currentTask.getId(),
                                        status);

                                // waiting: reset start time and keep checking
                                if ("waiting".equals(status)) {
                                    startTime.set(System.currentTimeMillis());
                                    return;
                                }

                                // timeout
                                if (currentTime - startTime.get()
                                        > TimeUnit.MINUTES.toMillis(maxWaitMinutes)) {
                                    log.debug(
                                            "Task timed out after {} minutes. Resetting to IDLE. VM: {}/{}/{}, Agent: {}",
                                            maxWaitMinutes,
                                            nsId,
                                            mciId,
                                            vmId,
                                            agent);

                                    if (agent == Agent.TELEGRAF) {
                                        vmService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        vmService.updateLogAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
                                    }

                                    log.warn("Timeout occurred for agent {}", agent);
                                    ScheduledFuture<?> scheduledFuture = futureRef.get();
                                    if (scheduledFuture != null) {
                                        scheduledFuture.cancel(false);
                                    }
                                    return;
                                }

                                // success case
                                if ("success".equals(status)) {
                                    log.debug("Task successful for agent {}", agent);
                                    if (agent == Agent.TELEGRAF) {
                                        vmService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FINISHED);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        vmService.updateLogAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FINISHED);
                                    }

                                    ScheduledFuture<?> scheduledFuture = futureRef.get();
                                    if (scheduledFuture != null) {
                                        scheduledFuture.cancel(false);
                                    }
                                    return;
                                }

                                // failed case
                                if ("error".equals(status)
                                        || "failed".equals(status)
                                        || "stopped".equals(status)) {
                                    log.debug("Task failed for agent {}", agent);

                                    if (agent == Agent.TELEGRAF) {
                                        vmService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FAILED);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        vmService.updateLogAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FAILED);
                                    }

                                    ScheduledFuture<?> scheduledFuture = futureRef.get();
                                    if (scheduledFuture != null) {
                                        scheduledFuture.cancel(false);
                                    }
                                    return;
                                }

                                // running or other statuses: continue checking
                            } catch (Exception e) {
                                // Do NOT cancel future on exception, only log
                                log.error(
                                        "Error while checking task status for agent {}: {}",
                                        agent,
                                        e.getMessage());
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
