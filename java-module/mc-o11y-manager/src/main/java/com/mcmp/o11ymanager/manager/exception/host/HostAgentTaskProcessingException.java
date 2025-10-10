package com.mcmp.o11ymanager.manager.exception.host;

import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import lombok.Getter;

@Getter
public class HostAgentTaskProcessingException extends BaseException {

    public HostAgentTaskProcessingException(
            String requestId, String hostId, String agentTypeName, VMAgentTaskStatus status) {
        super(
                requestId,
                ErrorCode.AGENT_TASK_IN_PROGRESS,
                String.format(
                        "Agent task %s is in progress on host (ID: %s). (Current status: %s)",
                        agentTypeName, hostId, status));
    }

    public HostAgentTaskProcessingException(String requestId, String idsSummary) {
        super(
                requestId,
                ErrorCode.AGENT_TASK_IN_PROGRESS,
                String.format("Hosts with agent tasks in progress: %s", idsSummary));
    }
}
