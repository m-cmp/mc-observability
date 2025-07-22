package com.mcmp.o11ymanager.event;

import com.mcmp.o11ymanager.enums.AgentAction;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class AgentHistoryFailEvent extends BaseDomainEvent {

  private final String requestId;
  private final AgentAction agentAction;
  private final String nsId;
  private final String mciId;
  private final String targetId;
  private final String reason;
}
