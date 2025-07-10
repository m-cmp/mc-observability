package com.innogrid.tabcloudit.o11ymanager.dto.host;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostConnectionDTO {

  private String hostId;
  private String ip;
  private String userId;
  private String password;
  private int port;
}
