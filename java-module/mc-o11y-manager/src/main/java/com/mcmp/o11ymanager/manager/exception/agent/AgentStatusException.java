package com.mcmp.o11ymanager.manager.exception.agent;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class
AgentStatusException extends BaseException {

  public AgentStatusException(String requestId, String message, Agent agent) {
    super(requestId, ErrorCode.AGENT_STATUS_ERROR,
        String.format("Agent [%s] 상태 변화에 실패했습니다.", agent.getName()));
  }
}
