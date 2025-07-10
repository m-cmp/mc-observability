package com.innogrid.tabcloudit.o11ymanager.service.interfaces;

import com.innogrid.tabcloudit.o11ymanager.dto.host.HostConnectionDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostCreateDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostUpdateDTO;

import com.innogrid.tabcloudit.o11ymanager.exception.agent.LogAgentNotInstalled;
import com.innogrid.tabcloudit.o11ymanager.exception.agent.MonitoringAgentNotInstalled;
import com.innogrid.tabcloudit.o11ymanager.exception.host.HostAgentTaskProcessingException;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import java.util.List;

public interface HostService {

  List<HostDTO> list();

  HostDTO findById(String id);

  HostDTO create(HostCreateDTO hostDTO, String ip);

  HostDTO update(String id, HostUpdateDTO hostDTO);

  void deleteById(String id);

  boolean existsById(String id);

  void validateUniqueIpPort(String ip, int port);

  void isIdleMonitoringAgent(String hostId) throws HostAgentTaskProcessingException;
  void isIdleLogAgent(String hostId) throws HostAgentTaskProcessingException;

  void isLogAgentInstalled(String hostId) throws LogAgentNotInstalled;
  void isMonitoringAgentInstalled(String hostId) throws MonitoringAgentNotInstalled;

  void updateMonitoringAgentTaskStatus(String hostId, HostAgentTaskStatus status);
  void updateLogAgentTaskStatus(String hostId, HostAgentTaskStatus status);

  void updateMonitoringAgentConfigGitHash(String hostId, String commitHash);
  void updateLogAgentConfigGitHash(String hostId, String commitHash);

  HostConnectionDTO getHostConnectionInfo(String hostId) throws Exception;

  void updateHostAgentTaskStatusAndTaskId(String hostId, HostAgentTaskStatus status, String taskId);

  void updateLogAgentTaskStatusAndTaskId(String hostId, HostAgentTaskStatus status, String taskId);




}
