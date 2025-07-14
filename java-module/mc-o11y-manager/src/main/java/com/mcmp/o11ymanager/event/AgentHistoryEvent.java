package com.mcmp.o11ymanager.event;

import com.mcmp.o11ymanager.enums.AgentAction;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class AgentHistoryEvent extends BaseDomainEvent{

    private final String requestId;
    private final AgentAction agentAction;
    private final String hostId;
    private final String requestUserId;
    private final String reason;
}
