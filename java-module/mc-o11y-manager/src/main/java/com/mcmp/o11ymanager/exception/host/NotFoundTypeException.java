package com.mcmp.o11ymanager.exception.host;

import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class NotFoundTypeException extends BaseException {
    public NotFoundTypeException(String requestId, Agent agent) {
            super(requestId, ErrorCode.AGENT_CONFIG_NOT_FOUND,
                    String.format("올바르지 않은 에이전트 타입입니다!", requestId, agent.getName()));
    }
}
