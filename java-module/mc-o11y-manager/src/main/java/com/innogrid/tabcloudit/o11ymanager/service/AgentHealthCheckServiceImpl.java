package com.innogrid.tabcloudit.o11ymanager.service;

import com.innogrid.tabcloudit.o11ymanager.dto.host.HostConnectionDTO;
import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.enums.AgentServiceStatus;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.AgentCommandResult;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;

import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.SshConnection;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.AgentHealthCheckService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.SshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentHealthCheckServiceImpl implements AgentHealthCheckService {
  @Value("${deploy.site-code}")
  private String deploySiteCode;

  private final SshService sshService;

  @Value("${health.host-connection-check-timeout:5000}")
  private int hostConnectionCheckTimeout;

  private String getTelegrafServiceName() {
    return "cmp-telegraf-" + deploySiteCode + ".service";
  }

  private String getFluentBitServiceName() {
    return "cmp-fluent-bit-" + deploySiteCode + ".service";
  }
  
  @Override
  public HostStatus getHostStatus(String ip, int port) {
    if (isTCPPortSuccessful(ip, port)) {
      return HostStatus.RUNNING;
    }
    return HostStatus.FAILED;
  }

  @Override
  public String getHostname(HostConnectionDTO connection) {
    String command = "hostname";
    return sshService.runCommand(connection.getIp(), connection.getPort(),
            connection.getUserId(), connection.getPassword(), command);
  }

  @Override
  public String getMonitoringAgentVersion(HostConnectionDTO connection) {
    String command = "cat /cmp-agent/sites/" + deploySiteCode + "/telegraf/version";
    return sshService.runCommand(connection.getIp(), connection.getPort(),
        connection.getUserId(), connection.getPassword(), command);
  }

  @Override
  public String getLogAgentVersion(HostConnectionDTO connection) {
    String command = "cat /cmp-agent/sites/" + deploySiteCode + "/fluent-bit/version";
    return sshService.runCommand(connection.getIp(), connection.getPort(),
        connection.getUserId(), connection.getPassword(),
        command);
  }

  private String getUnixTimestamp(HostConnectionDTO connection) {
    String command = "date +%s";
    return sshService.runCommand(connection.getIp(), connection.getPort(),
            connection.getUserId(), connection.getPassword(),
            command);
  }

  private String getUnixTimeDiff(HostConnectionDTO connection) {
    long beforeTime = Instant.now().getEpochSecond();
    long remoteTime = Long.parseLong(getUnixTimestamp(connection));
    long afterTime = Instant.now().getEpochSecond();
    long networkDelay = (afterTime - beforeTime) / 2;

    return String.valueOf(beforeTime - (remoteTime - networkDelay));
  }

  @Override
  public void writeUnixTimeDiff(HostConnectionDTO connection) {
    String command = "echo 'TIME_DIFF_SECONDS=\"" + getUnixTimeDiff(connection) + "\"' > /cmp-agent/sites/" + deploySiteCode + "/time.env";
    sshService.runCommand(connection.getIp(), connection.getPort(),
            connection.getUserId(), connection.getPassword(),
            command);
  }

  @Override
  public String getAgentServiceStatus(Agent agent, HostEntity host, HostConnectionDTO connection) {
    String command1 = "";
    String command2 = "";
    HostAgentTaskStatus hostAgentTaskStatus = null;

    SshConnection sshConnection = sshService.getConnection(connection.getIp(), connection.getPort(),
        connection.getUserId(), connection.getPassword());
    if (sshConnection == null) {
      return AgentServiceStatus.FAILED.toString();
    }

    if (agent.equals(Agent.TELEGRAF)) {
      command1 = "echo '" + connection.getPassword() + "' | sudo -S -p '' systemctl status "
          + getTelegrafServiceName();
      command2 = "echo '" + connection.getPassword() + "' | sudo -S -p '' systemctl is-active "
          + getTelegrafServiceName();
      hostAgentTaskStatus = host.getHost_monitoring_agent_task_status();
    } else if (agent.equals(Agent.FLUENT_BIT)) {
      command1 = "echo '" + connection.getPassword() + "' | sudo -S -p '' systemctl status "
          + getFluentBitServiceName();
      command2 = "echo '" + connection.getPassword() + "' | sudo -S -p '' systemctl is-active "
          + getFluentBitServiceName();
      hostAgentTaskStatus = host.getHost_log_agent_task_status();
    }

    if (hostAgentTaskStatus != null && hostAgentTaskStatus != HostAgentTaskStatus.IDLE) {
      return hostAgentTaskStatus.toString();
    }

    try {
      AgentCommandResult agentCommandResult1 = sshService.runCommandWithResult(connection.getIp(),
          connection.getPort(), connection.getUserId(), connection.getPassword(), command1);
      Integer exitCode1 = agentCommandResult1.getExitCode();

      AgentCommandResult agentCommandResult2 = sshService.runCommandWithResult(connection.getIp(),
          connection.getPort(), connection.getUserId(), connection.getPassword(), command2);
      String output2 = agentCommandResult2.getOutput();

      if (exitCode1.equals(4)) {
        return AgentServiceStatus.NOT_EXIST.toString();
      } else {
        if ("active".equalsIgnoreCase(output2)) {
          return AgentServiceStatus.ACTIVE.toString();
        } else if ("activating".equalsIgnoreCase(output2)) {
          return AgentServiceStatus.ACTIVATING.toString();
        } else if ("inactive".equalsIgnoreCase(output2)) {
          return AgentServiceStatus.INACTIVE.toString();
        } else if ("deactivating".equalsIgnoreCase(output2)) {
          return AgentServiceStatus.DEACTIVATING.toString();
        } else if ("reloading".equalsIgnoreCase(output2)) {
          return AgentServiceStatus.RELOADING.toString();
        }
      }
    } catch (Exception e) {
      log.error("Error while checking service status of host {}: {}", connection.getIp(),
          e.getMessage());
      sshService.removeConnection(connection.getIp(), connection.getPort(), connection.getUserId());
    }

    return AgentServiceStatus.FAILED.toString();
  }

  @Override
  public boolean enableDisableAgentStatus(Agent agent, HostEntity host, boolean isEnable) {
    SshConnection sshConnection = sshService.getConnection(host.getIp(), host.getPort(),
        host.getUser(), host.getPassword());
    if (sshConnection == null) {
      return false;
    }

    if (isEnable && sshService.isEnable(agent, host.getIp(), host.getPort(), host.getUser(),
        host.getPassword(), sshConnection)) {
      return true;
    }

    String enableDisable = isEnable ? "enable" : "disable";
    String changeStatus = isEnable ? "start" : "stop";

    String command = switch (agent) {
      case TELEGRAF ->
          "echo '" + host.getPassword() + "' | sudo -S -p '' systemctl " + enableDisable + " "
              + getTelegrafServiceName() + " && " +
              "systemctl " + changeStatus + " " + getTelegrafServiceName();
      case FLUENT_BIT ->
          "echo '" + host.getPassword() + "' | sudo -S -p '' systemctl " + enableDisable + " "
              + getFluentBitServiceName() + " && " +
              "systemctl " + changeStatus + " " + getFluentBitServiceName();
    };

    try {
      AgentCommandResult agentCommandResult = sshService.runCommandWithResult(host.getIp(),
          host.getPort(), host.getPassword(), host.getUser(), command);
      return agentCommandResult.getExitCode()
          != SshServiceImpl.SSH_COMMAND_EXECUTE_FAILED_CODE; // TODO 추후 방법 강구
    } catch (Exception e) {
      log.error("Failed to {} agent: {}", isEnable ? "enable" : "disable", agent, e);
      sshService.removeConnection(host.getIp(), host.getPort(), host.getUser());
      return false;
    }
  }

  @Override
  public boolean restartAgent(Agent agent, HostEntity host) {
    SshConnection sshConnection = sshService.getConnection(host.getIp(), host.getPort(),
        host.getUser(), host.getPassword());

    if (sshConnection == null) {
      return false;
    }

    if (!sshService.isEnable(agent, host.getIp(), host.getPort(), host.getUser(),
        host.getPassword(), sshConnection)) {
      throw new IllegalStateException("Agent is not enabled, cannot restart.");
    }

    String command = switch (agent) {
      case TELEGRAF -> "echo '" + host.getPassword() + "' | sudo -S -p '' systemctl restart "
          + getTelegrafServiceName();
      case FLUENT_BIT -> "echo '" + host.getPassword() + "' | sudo -S -p '' systemctl restart "
          + getFluentBitServiceName();
    };

    try {
      AgentCommandResult agentCommandResult = sshService.runCommandWithResult(host.getIp(),
          host.getPort(), host.getPassword(), host.getUser(), command);
      return agentCommandResult.getExitCode() != SshServiceImpl.SSH_COMMAND_EXECUTE_FAILED_CODE;
    } catch (Exception e) {
      log.error("Failed to restart agent: {}", agent, e);
      sshService.removeConnection(host.getIp(), host.getPort(), host.getUser());
      return false;
    }
  }

  private boolean isTCPPortSuccessful(String ip, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(ip, port), hostConnectionCheckTimeout);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
