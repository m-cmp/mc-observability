package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.model.host.VMStatus;

public interface TcpService {
    boolean isConnect(String ip, int port);

    VMStatus checkServerStatus(String ip, int port);
}
