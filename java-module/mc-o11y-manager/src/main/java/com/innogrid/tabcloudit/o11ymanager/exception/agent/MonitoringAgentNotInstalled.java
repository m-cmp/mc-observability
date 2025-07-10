package com.innogrid.tabcloudit.o11ymanager.exception.agent;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class MonitoringAgentNotInstalled extends BaseException {

  public MonitoringAgentNotInstalled(String requestId, String hostId) {
    super(requestId, ErrorCode.MONITORING_AGENT_NOT_EXIST,
        String.format("호스트(ID: %s)에 에이전트가 존재하지 않습니다.", hostId));

  }
}
