package com.innogrid.tabcloudit.o11ymanager.facade;

import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.enums.AgentAction;
import com.innogrid.tabcloudit.o11ymanager.enums.SemaphoreInstallMethod;
import com.innogrid.tabcloudit.o11ymanager.event.AgentHistoryEvent;
import com.innogrid.tabcloudit.o11ymanager.event.AgentHistoryFailEvent;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Project;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Task;
import com.innogrid.tabcloudit.o11ymanager.port.SemaphorePort;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.HostService;
import jakarta.transaction.Transactional;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SchedulerFacadeService {

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
  private final SemaphorePort semaphorePort;
  private final GitFacadeService gitFacadeService;
  private final ApplicationEventPublisher event;
  private final HostService hostService;

  @Value("${feign.semaphore.project-name}")
  private String projectName;

  @Value("${feign.semaphore.task-check-scheduler.check-interval-sec:5}")
  private int checkIntervalSec;

  @Value("${feign.semaphore.task-check-scheduler.max-wait-minutes:30}")
  private int maxWaitMinutes;

  public void scheduleTaskStatusCheck(String requestId, Integer taskId, String hostId,
      SemaphoreInstallMethod method, Agent agent, String requestUserId) {
    AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

    ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
      try {
        long currentTime = System.currentTimeMillis();

        AgentAction action;
        boolean isSuccess;
        Project project = semaphorePort.getProjectByName(projectName);
        Task currentTask = semaphorePort.getTask(project.getId(), taskId);

        log.info("🔨🔨 --------------------Task Status-------------------- 🔨🔨");
        log.debug(
            "Task Status - Request ID: {}, Host ID: {}, Agent: {}, Install Method: {}, Task ID: {}, Status: {}, Request User ID: {}",
            requestId, hostId, agent, method, currentTask.getId(), currentTask.getStatus(),
            requestUserId);

        if ("waiting".equals(currentTask.getStatus())) {
          startTime.set(System.currentTimeMillis());
          return;
        }

        // 타임아웃의 경우
        if (currentTime - startTime.get() > TimeUnit.MINUTES.toMillis(maxWaitMinutes)) {
          log.warn(
              "Task timed out after {} minutes. Resetting status to IDLE. Host ID: {}, Agent: {}",
              maxWaitMinutes, hostId, agent);

          switch (agent) {
            case TELEGRAF ->
                hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.IDLE);
            case FLUENT_BIT ->
                hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.IDLE);
          }

          throw new TimeoutException(
              "Task max wait timed out after " + maxWaitMinutes + " minutes");
        }

        // 성공시
        if ("success".equals(currentTask.getStatus())) {
          action = getAgentActionFinished(method, agent);
          log.debug(action.toString());
          isSuccess = true;
          log.debug(isSuccess ? "Task successful" : "Task failed");
        } else if ("error".equals(currentTask.getStatus())) {
          action = getAgentActionFailed(method, agent);
          isSuccess = false;
          revertLastCommit(requestId, hostId, action);
        } else {
          return;
        }

        if (action != null) {
          log.debug(
              "Updating Agent History - Request ID: {}, Host ID: {}, Agent: {}, Action: {}, Request User ID: {}",
              requestId, hostId, agent, action, requestUserId);
          switch (agent) {
            case TELEGRAF ->
                hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.IDLE);
            case FLUENT_BIT ->
                hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.IDLE);
          }

          log.info("🔨🔨 --------------------task end-------------------- 🔨🔨");

          if (isSuccess) {
            AgentHistoryEvent successEvent = new AgentHistoryEvent(requestId, action, hostId,
                requestUserId, null);
            event.publishEvent(successEvent);
          } else {
            AgentHistoryFailEvent failureEvent = new AgentHistoryFailEvent(requestId, action,
                hostId, requestUserId,
                "호스트에서 작업을 수행하던 중 실패하였습니다.");
            event.publishEvent(failureEvent);
          }
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
              "Updating Agent History - Request ID: {}, Host ID: {}, Agent: {}, Action: {}, Request User ID: {}",
              requestId, hostId, agent, action, requestUserId);

          switch (agent) {
            case TELEGRAF ->
                hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.IDLE);
            case FLUENT_BIT ->
                hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.IDLE);
          }

        }
      }
    }, 0, checkIntervalSec, TimeUnit.SECONDS);

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

  // TODO  해결 필요
  private void revertLastCommit(String requestId, String hostId, AgentAction action) {
    if (action == AgentAction.MONITORING_AGENT_UPDATE_FAILED ||
        action == AgentAction.MONITORING_AGENT_CONFIG_ROLLBACK_FAILED) {
      gitFacadeService.revertLastCommit(requestId, hostId, Agent.TELEGRAF);
    } else if (action == AgentAction.LOG_AGENT_UPDATE_FAILED ||
        action == AgentAction.LOG_AGENT_CONFIG_ROLLBACK_FAILED) {
      gitFacadeService.revertLastCommit(requestId, hostId, Agent.FLUENT_BIT);
    }
  }
}
