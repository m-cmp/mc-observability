package com.mcmp.o11ymanager.oldService.domain.interfaces;

import com.mcmp.o11ymanager.dto.host.HostConnectionDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.exception.agent.LogAgentNotInstalled;
import com.mcmp.o11ymanager.exception.agent.MonitoringAgentNotInstalled;
import com.mcmp.o11ymanager.exception.host.HostAgentTaskProcessingException;
import com.mcmp.o11ymanager.model.host.HostAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import java.util.List;
import java.util.Optional;

public interface TargetService {

  Optional<TargetEntity> findById(String targetId);

  boolean existsById(String targetId);

  TargetDTO get(String nsId, String mciId, String targetId);

  TargetDTO getByNsMci(String nsId, String mciId);

  List<TargetDTO> list();

  TargetDTO post(String nsId, String mciId, String targetId, TargetRegisterDTO dto);

  TargetDTO put(String targetId, String nsId, String mciId, TargetUpdateDTO request);

  void delete(String targetId, String nsId, String mciId);

  
  
  //agent install
  void isIdleMonitoringAgent(String targetId) throws HostAgentTaskProcessingException;
  void isIdleLogAgent(String targetId) throws HostAgentTaskProcessingException;

  void isLogAgentInstalled(String targetId) throws LogAgentNotInstalled;
  void isMonitoringAgentInstalled(String targetId) throws MonitoringAgentNotInstalled;

  void updateMonitoringAgentTaskStatus(String targetId, TargetAgentTaskStatus status);
  void updateLogAgentTaskStatus(String targetId, TargetAgentTaskStatus status);

  void updateMonitoringAgentConfigGitHash(String targetId, String commitHash);
  void updateLogAgentConfigGitHash(String targetId, String commitHash);

  TargetRegisterDTO getTargetInfo(String targetId) throws Exception;

  TargetRegisterDTO.AccessInfoDTO getAccessInfo(String targetId) throws Exception;

  void updateMonitoringAgentTaskStatusAndTaskId(String targetId, TargetAgentTaskStatus status, String taskId);

  void updateLogAgentTaskStatusAndTaskId(String targetId, TargetAgentTaskStatus status, String taskId);


}
