package com.innogrid.tabcloudit.o11ymanager.model.tcp;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PingRequest {
  private String ip;
  private int port;
}
