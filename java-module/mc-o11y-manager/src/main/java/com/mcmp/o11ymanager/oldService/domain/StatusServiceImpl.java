package com.mcmp.o11ymanager.oldService.domain;

import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.model.host.HostAgentTaskStatus;
import com.mcmp.o11ymanager.oldService.domain.interfaces.StatusService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatusServiceImpl implements StatusService {

  private final HostDomainService hostDomainService;
  private static final Lock agentTaskStatusLock = new ReentrantLock();

  @Override
  public void updateHostAgentTaskStatus(String requestId, Integer taskId,
      HostAgentTaskStatus hostAgentTaskStatus,
      String hostId, Agent agent) {
    log.debug(
        "Updating Agent Task Status - Request ID: {}, Host ID: {}, Agent: {}, Agent Task Status: {}",
        requestId, hostId, agent, hostAgentTaskStatus);

    HostEntity updateHost = HostEntity.builder()
        .id(hostId)
        .build();

    if (agent.equals(Agent.TELEGRAF)) {
      updateHost.setHost_monitoring_agent_task_status(hostAgentTaskStatus);
    } else if (agent.equals(Agent.FLUENT_BIT)) {
      updateHost.setHost_log_agent_task_status(hostAgentTaskStatus);
    }

    if (hostAgentTaskStatus == HostAgentTaskStatus.IDLE) {
      if (agent.equals(Agent.TELEGRAF)) {
        updateHost.setHost_monitoring_agent_task_id("");
      } else if (agent.equals(Agent.FLUENT_BIT)) {
        updateHost.setHost_log_agent_task_id("");
      }
    } else if (taskId != null) {
      if (agent.equals(Agent.TELEGRAF)) {
        updateHost.setHost_monitoring_agent_task_id(taskId.toString());
      } else if (agent.equals(Agent.FLUENT_BIT)) {
        updateHost.setHost_log_agent_task_id(taskId.toString());
      }
    }

    hostDomainService.updateHost(updateHost);
  }

  @Override
  public void resetHostAgentTaskStatus(String requestId, String hostId, Agent agent) {
    if (hostId == null || hostId.isEmpty() || agent == null) {
      return;
    }

    try {
      agentTaskStatusLock.lock();
      updateHostAgentTaskStatus(requestId, null, HostAgentTaskStatus.IDLE, hostId, agent);
    } finally {
      agentTaskStatusLock.unlock();
    }
  }
}
