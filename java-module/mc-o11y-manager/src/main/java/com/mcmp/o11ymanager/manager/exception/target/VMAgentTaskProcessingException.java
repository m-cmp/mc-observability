package com.mcmp.o11ymanager.manager.exception.vm;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import lombok.Getter;

@Getter
public class VMAgentTaskProcessingException extends BaseException {

  public VMAgentTaskProcessingException(
      String requestId, String vmId, String agentTypeName, VMAgentTaskStatus status) {
    super(
        requestId,
        ErrorCode.AGENT_TASK_IN_PROGRESS,
        String.format(
            "Agent task %s is in progress on host (ID: %s). (Current status: %s)",
            agentTypeName, vmId, status));
  }

  public VMAgentTaskProcessingException(String requestId, String idsSummary) {
    super(
        requestId,
        ErrorCode.AGENT_TASK_IN_PROGRESS,
        String.format("Hosts with agent tasks in progress: %s", idsSummary));
  }

}
