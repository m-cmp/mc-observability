package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.model.host.HostStatus;

public interface TcpService {
    boolean isConnect(String ip, int port);

    HostStatus checkServerStatus(String ip, int port);
}
