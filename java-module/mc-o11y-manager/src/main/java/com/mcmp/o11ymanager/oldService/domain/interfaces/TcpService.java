package com.mcmp.o11ymanager.oldService.domain.interfaces;

import com.mcmp.o11ymanager.model.host.HostStatus;

public interface TcpService {
  boolean isConnect(String ip, int port);

  HostStatus checkServerStatus(String ip, int port);
}
