package com.mcmp.o11ymanager.manager.exception.target;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import com.mcmp.o11ymanager.manager.model.host.TargetAgentTaskStatus;
import lombok.Getter;

@Getter
public class TargetAgentTaskProcessingException extends BaseException {
    public TargetAgentTaskProcessingException(
            String requestId, String targetId, String agentTypeName, TargetAgentTaskStatus status) {
        super(
                requestId,
                ErrorCode.AGENT_TASK_IN_PROGRESS,
                String.format(
                        "호스트(ID: %s)에서 %s 에이전트 작업이 진행 중입니다. (현재 상태: %s)",
                        targetId, agentTypeName, status));
    }

    public TargetAgentTaskProcessingException(String requestId, String idsSummary) {
        super(
                requestId,
                ErrorCode.AGENT_TASK_IN_PROGRESS,
                String.format("에이전트 작업이 진행 중인 호스트들: %s", idsSummary));
    }
}
