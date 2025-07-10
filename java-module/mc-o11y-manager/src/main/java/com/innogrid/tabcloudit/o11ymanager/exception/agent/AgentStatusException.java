package com.innogrid.tabcloudit.o11ymanager.exception.agent;

import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class AgentStatusException extends BaseException {

  public AgentStatusException(String requestId, String message, Agent agent) {
    super(requestId, ErrorCode.AGENT_STATUS_ERROR,
        String.format("Agent [%s] 상태 변화에 실패했습니다.", agent.getName()));
  }
}
