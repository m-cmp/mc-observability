package com.mcmp.o11ymanager.manager.exception.host;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class BadRequestException extends BaseException {
    public BadRequestException(String requestId, String hostId, Agent agent, String message) {
        super(requestId, ErrorCode.valueOf(message));
    }
}
