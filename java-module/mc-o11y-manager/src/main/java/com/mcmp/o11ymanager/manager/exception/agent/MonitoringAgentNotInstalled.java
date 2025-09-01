package com.mcmp.o11ymanager.manager.exception.agent;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;

public class MonitoringAgentNotInstalled extends BaseException {

  public MonitoringAgentNotInstalled(String requestId, String hostId) {
    super(requestId, ErrorCode.MONITORING_AGENT_NOT_EXIST,
        String.format("호스트(ID: %s)에 에이전트가 존재하지 않습니다.", hostId));

  }
}
