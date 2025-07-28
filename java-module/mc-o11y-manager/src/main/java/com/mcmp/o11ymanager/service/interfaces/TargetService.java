package com.mcmp.o11ymanager.service.interfaces;


import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.exception.agent.LogAgentNotInstalled;
import com.mcmp.o11ymanager.exception.agent.MonitoringAgentNotInstalled;
import com.mcmp.o11ymanager.exception.host.HostAgentTaskProcessingException;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;

import com.mcmp.o11ymanager.model.host.TargetStatus;
import java.util.List;

public interface TargetService {

  TargetDTO get(String nsId, String mciId, String targetId);

  List<TargetDTO> getByNsMci(String nsId, String mciId);

  List<TargetDTO> list();

  TargetDTO post(String nsId, String mciId, String targetId, TargetStatus targetStatus, TargetRequestDTO dto);

  TargetDTO put(String nsId, String mciId, String targetId, TargetRequestDTO dto);

  void delete(String nsId, String mciId, String targetId);

  void isIdleMonitoringAgent(String nsId, String mciId, String targetId) throws HostAgentTaskProcessingException;
  void isIdleLogAgent(String nsId, String mciId, String targetId) throws HostAgentTaskProcessingException;

  void isLogAgentInstalled(String nsId, String mciId, String targetId) throws LogAgentNotInstalled;
  void isMonitoringAgentInstalled(String nsId, String mciId, String targetId) throws MonitoringAgentNotInstalled;

  void updateMonitoringAgentTaskStatus(String nsId, String mciId, String targetId, TargetAgentTaskStatus status);
  void updateLogAgentTaskStatus(String nsId, String mciId, String targetId, TargetAgentTaskStatus status);

  void updateMonitoringAgentConfigGitHash(String nsId, String mciId, String targetId, String commitHash);
  void updateLogAgentConfigGitHash(String nsId, String mciId, String targetId, String commitHash);

  void updateMonitoringAgentTaskStatusAndTaskId(String nsId, String mciId, String targetId, TargetAgentTaskStatus status, String taskId);

  void updateLogAgentTaskStatusAndTaskId(String nsId, String mciId, String targetId, TargetAgentTaskStatus status, String taskId);
}
