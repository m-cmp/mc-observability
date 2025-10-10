package com.mcmp.o11ymanager.manager.exception.host;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class NotFoundTypeException extends BaseException {

  public NotFoundTypeException(String requestId, Agent agent) {
    super(
        requestId,
        ErrorCode.AGENT_CONFIG_NOT_FOUND,
        String.format("Invalid agent type!", requestId, agent.getName()));
  }

}
