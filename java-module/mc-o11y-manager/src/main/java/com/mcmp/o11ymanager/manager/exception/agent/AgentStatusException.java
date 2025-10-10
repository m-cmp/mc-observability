package com.mcmp.o11ymanager.manager.exception.agent;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class AgentStatusException extends BaseException {

  public AgentStatusException(String requestId, String message, Agent agent) {
    super(
        requestId,
        ErrorCode.AGENT_STATUS_ERROR,
        String.format("Failed to change status of agent [%s].", agent.getName()));

  }
}
