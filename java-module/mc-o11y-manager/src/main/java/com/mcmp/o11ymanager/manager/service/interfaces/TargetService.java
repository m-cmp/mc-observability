package com.mcmp.o11ymanager.manager.service.interfaces;


import com.mcmp.o11ymanager.manager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.manager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.manager.exception.agent.LogAgentNotInstalled;
import com.mcmp.o11ymanager.manager.exception.agent.MonitoringAgentNotInstalled;
import com.mcmp.o11ymanager.manager.exception.host.HostAgentTaskProcessingException;
import com.mcmp.o11ymanager.manager.model.host.TargetAgentTaskStatus;

import com.mcmp.o11ymanager.manager.model.host.TargetStatus;
import java.util.List;

public interface TargetService {

  TargetDTO get(String nsId, String mciId, String targetId);

  List<TargetDTO> getByNsMci(String nsId, String mciId);

  TargetDTO getByNsVm(String nsId, String targetId);

  List<TargetDTO> list();

  TargetDTO post(String nsId, String mciId, String targetId, TargetStatus targetStatus, TargetRequestDTO dto, Long influxSeq);

  TargetDTO put(String nsId, String mciId, String targetId, TargetRequestDTO dto);

  void delete(String nsId, String mciId, String targetId);

  void isIdleMonitoringAgent(String nsId, String mciId, String targetId) throws HostAgentTaskProcessingException;
  void isIdleLogAgent(String nsId, String mciId, String targetId) throws HostAgentTaskProcessingException;

  void isLogAgentInstalled(String nsId, String mciId, String targetId) throws LogAgentNotInstalled;
  void isMonitoringAgentInstalled(String nsId, String mciId, String targetId) throws MonitoringAgentNotInstalled;

  void updateMonitoringAgentTaskStatus(String nsId, String mciId, String targetId, TargetAgentTaskStatus status);
  void updateLogAgentTaskStatus(String nsId, String mciId, String targetId, TargetAgentTaskStatus status);

  void updateMonitoringAgentTaskStatusAndTaskId(String nsId, String mciId, String targetId, TargetAgentTaskStatus status, String taskId);

  void updateLogAgentTaskStatusAndTaskId(String nsId, String mciId, String targetId, TargetAgentTaskStatus status, String taskId);

  List<String> getTargetIds(String nsId, String mciId);
  Long getInfluxId(String nsId, String mciId);

}
