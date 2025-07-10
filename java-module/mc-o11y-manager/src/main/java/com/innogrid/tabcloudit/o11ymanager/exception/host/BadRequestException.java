package com.innogrid.tabcloudit.o11ymanager.exception.host;

import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;
import com.innogrid.tabcloudit.o11ymanager.service.HostService;
import lombok.Getter;

@Getter
public class BadRequestException extends BaseException {
    public BadRequestException(String requestId, String hostId, Agent agent, String message) {
        super(requestId, ErrorCode.valueOf(message));
    }
}
