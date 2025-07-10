package com.innogrid.tabcloudit.o11ymanager.service.interfaces;

import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.enums.SemaphoreInstallMethod;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import java.util.List;

public interface StatusService {

  void updateHostAgentTaskStatus(String requestId, Integer taskId,
      HostAgentTaskStatus hostAgentTaskStatus,
      String hostId, Agent agent);


  void resetHostAgentTaskStatus(String requestId, String hostId, Agent agent);



}
