package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.dto.host.HostDTO;
import com.mcmp.o11ymanager.facade.SchedulerFacadeService;
import com.mcmp.o11ymanager.service.domain.HostDomainService;
import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.model.host.HostAgentTaskStatus;
import com.mcmp.o11ymanager.enums.SemaphoreInstallMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostService {

  private final HostDomainService hostDomainService;
  private final HostServiceImpl hostService;
  private final SchedulerFacadeService schedulerFacadeService;

  @Transactional
  public void resetAllHostAgentTaskStatus() {
    List<HostEntity> hostList = hostService.list().stream().map(HostDTO::toEntity).toList();

    for (HostEntity host : hostList) {
      HostAgentTaskStatus hostMonitoringAgentTaskStatus = host.getHost_monitoring_agent_task_status();
      HostAgentTaskStatus hostLogAgentTaskStatus = host.getHost_log_agent_task_status();
      SemaphoreInstallMethod monitoringMethod = null;
      SemaphoreInstallMethod logMethod = null;

      if (hostMonitoringAgentTaskStatus != null) {
        if (hostMonitoringAgentTaskStatus.equals(HostAgentTaskStatus.INSTALLING)) {
          monitoringMethod = SemaphoreInstallMethod.INSTALL;
        } else if (hostMonitoringAgentTaskStatus.equals(HostAgentTaskStatus.UNINSTALLING)) {
          monitoringMethod = SemaphoreInstallMethod.UNINSTALL;
        } else if (hostMonitoringAgentTaskStatus.equals(HostAgentTaskStatus.RESTARTING)) {
          monitoringMethod = SemaphoreInstallMethod.RESTART;
        } else if (hostMonitoringAgentTaskStatus.equals(HostAgentTaskStatus.UPDATING_CONFIG)) {
          monitoringMethod = SemaphoreInstallMethod.CONFIG_UPDATE;
        } else if (hostMonitoringAgentTaskStatus.equals(HostAgentTaskStatus.ROLLING_BACK_CONFIG)) {
          monitoringMethod = SemaphoreInstallMethod.ROLLBACK_CONFIG;
        }
      } else {
        host.setHost_monitoring_agent_task_status(HostAgentTaskStatus.IDLE);
        host.setHost_log_agent_task_id("");
        hostDomainService.updateHost(host);
        log.info(host.getMonitoring_agent_version());
      }

      if (hostLogAgentTaskStatus != null) {
        if (hostLogAgentTaskStatus.equals(HostAgentTaskStatus.INSTALLING)) {
          logMethod = SemaphoreInstallMethod.INSTALL;
        } else if (hostLogAgentTaskStatus.equals(HostAgentTaskStatus.UNINSTALLING)) {
          logMethod = SemaphoreInstallMethod.UNINSTALL;
        } else if (hostLogAgentTaskStatus.equals(HostAgentTaskStatus.RESTARTING)) {
          logMethod = SemaphoreInstallMethod.RESTART;
        } else if (hostLogAgentTaskStatus.equals(HostAgentTaskStatus.UPDATING_CONFIG)) {
          logMethod = SemaphoreInstallMethod.CONFIG_UPDATE;
        } else if (hostLogAgentTaskStatus.equals(HostAgentTaskStatus.ROLLING_BACK_CONFIG)) {
          logMethod = SemaphoreInstallMethod.ROLLBACK_CONFIG;
        }
      } else {
        host.setHost_monitoring_agent_task_status(HostAgentTaskStatus.IDLE);
        host.setHost_log_agent_task_id("");
        hostDomainService.updateHost(host);
      }

      if (host.getHost_monitoring_agent_task_id() != null && monitoringMethod != null) {
        if (host.getHost_monitoring_agent_task_id().isEmpty()) {
          host.setHost_monitoring_agent_task_status(HostAgentTaskStatus.IDLE);
          host.setHost_monitoring_agent_task_id("");
          hostDomainService.updateHost(host);
        } else {
          schedulerFacadeService.scheduleTaskStatusCheck("INIT",
              Integer.valueOf(host.getHost_monitoring_agent_task_id()),
              host.getId(), monitoringMethod, Agent.TELEGRAF, "");
        }
      } else {
        host.setHost_monitoring_agent_task_status(HostAgentTaskStatus.IDLE);
        host.setHost_monitoring_agent_task_id("");
        hostDomainService.updateHost(host);
      }

      if (host.getHost_log_agent_task_id() != null && logMethod != null) {
        if (host.getHost_log_agent_task_id().isEmpty()) {
          host.setHost_log_agent_task_status(HostAgentTaskStatus.IDLE);
          host.setHost_log_agent_task_id("");
          hostDomainService.updateHost(host);
        } else {
          schedulerFacadeService.scheduleTaskStatusCheck("INIT",
              Integer.valueOf(host.getHost_log_agent_task_id()),
              host.getId(), logMethod, Agent.FLUENT_BIT, "");
        }
      } else {
        host.setHost_log_agent_task_status(HostAgentTaskStatus.IDLE);
        host.setHost_log_agent_task_id("");
        hostDomainService.updateHost(host);
      }
    }
  }


}
