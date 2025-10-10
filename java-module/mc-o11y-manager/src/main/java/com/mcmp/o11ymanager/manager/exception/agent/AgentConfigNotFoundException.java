package com.mcmp.o11ymanager.manager.exception.agent;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;

public class AgentConfigNotFoundException extends BaseException {

  public AgentConfigNotFoundException(String requestId, String hostId, Agent agent) {
    super(
        requestId,
        ErrorCode.AGENT_CONFIG_NOT_FOUND,
        String.format("No %s agent configuration found on host (ID: %s).", agent.getName(),
            hostId));

  }
}
