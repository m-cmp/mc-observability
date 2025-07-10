package com.innogrid.tabcloudit.o11ymanager.exception.host;

import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class HostAgentTaskProcessingException extends BaseException {
    public HostAgentTaskProcessingException(String requestId, String hostId, String agentTypeName, HostAgentTaskStatus status) {
        super(
                requestId,
                ErrorCode.AGENT_TASK_IN_PROGRESS,
                String.format("호스트(ID: %s)에서 %s 에이전트 작업이 진행 중입니다. (현재 상태: %s)", hostId, agentTypeName, status)
        );
    }

    public HostAgentTaskProcessingException(String requestId, String idsSummary) {
        super(
                requestId,
                ErrorCode.AGENT_TASK_IN_PROGRESS,
                String.format("에이전트 작업이 진행 중인 호스트들: %s", idsSummary)
        );
    }
}
