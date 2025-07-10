package com.innogrid.tabcloudit.o11ymanager.service.interfaces;

import com.innogrid.tabcloudit.o11ymanager.model.host.HostStatus;

public interface TcpService {
  boolean isConnect(String ip, int port);

  HostStatus checkServerStatus(String ip, int port);
}
