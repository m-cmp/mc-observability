package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.model.host.HostAgentTaskStatus;
import java.util.List;

public interface StatusService {

  void updateHostAgentTaskStatus(String requestId, Integer taskId,
      HostAgentTaskStatus hostAgentTaskStatus,
      String hostId, Agent agent);


  void resetHostAgentTaskStatus(String requestId, String hostId, Agent agent);



}
