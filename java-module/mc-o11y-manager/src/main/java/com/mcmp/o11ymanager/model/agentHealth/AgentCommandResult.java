package com.mcmp.o11ymanager.model.agentHealth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AgentCommandResult {
    private final String output;
    private final String error;
    private final int exitCode;
}
