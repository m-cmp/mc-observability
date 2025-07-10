package com.innogrid.tabcloudit.o11ymanager.service.interfaces;

import com.innogrid.tabcloudit.o11ymanager.dto.host.HostConnectionDTO;
import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostStatus;

public interface AgentHealthCheckService {


    HostStatus getHostStatus(String ip, int port);

    String getHostname(HostConnectionDTO connection);

    String getMonitoringAgentVersion(HostConnectionDTO connection);

    String getLogAgentVersion(HostConnectionDTO connection);

    void writeUnixTimeDiff(HostConnectionDTO connection);

    String getAgentServiceStatus(Agent agent, HostEntity host, HostConnectionDTO connection);

    boolean enableDisableAgentStatus(Agent agent, HostEntity host, boolean isEnable);

    boolean restartAgent(Agent agent, HostEntity host);
}
