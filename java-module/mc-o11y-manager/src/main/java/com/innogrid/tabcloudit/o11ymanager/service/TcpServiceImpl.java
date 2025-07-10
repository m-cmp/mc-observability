package com.innogrid.tabcloudit.o11ymanager.service;

import com.innogrid.tabcloudit.o11ymanager.model.host.HostStatus;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.TcpService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TcpServiceImpl implements TcpService {

  @Value("${health.host-connection-check-timeout:5000}")
  private int hostConnectionCheckTimeout;

  public boolean isConnect(String ip, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(ip, port), hostConnectionCheckTimeout);
      return true;
    } catch (IOException e) {
      return false;
    }
  }


  public HostStatus checkServerStatus(String ip, int port) {
    if(isConnect(ip, port))
      return HostStatus.RUNNING;
    else
      return HostStatus.FAILED;
  }
}
