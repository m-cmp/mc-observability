package com.mcmp.o11ymanager.exception.agent;

import com.mcmp.o11ymanager.exception.host.BaseException;
import com.mcmp.o11ymanager.global.error.ErrorCode;

public class LogAgentNotInstalled extends BaseException {

  public LogAgentNotInstalled(String requestId, String hostId) {
    super(requestId, ErrorCode.LOG_AGENT_NOT_EXIST,
        String.format("호스트(ID: %s)에 에이전트가 존재하지 않습니다.", hostId));

  }
}
