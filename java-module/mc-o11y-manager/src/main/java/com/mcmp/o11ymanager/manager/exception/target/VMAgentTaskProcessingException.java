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
                        "호스트(ID: %s)에서 %s 에이전트 작업이 진행 중입니다. (현재 상태: %s)",
                        vmId, agentTypeName, status));
    }

    public VMAgentTaskProcessingException(String requestId, String idsSummary) {
        super(
                requestId,
                ErrorCode.AGENT_TASK_IN_PROGRESS,
                String.format("에이전트 작업이 진행 중인 호스트들: %s", idsSummary));
    }
}
