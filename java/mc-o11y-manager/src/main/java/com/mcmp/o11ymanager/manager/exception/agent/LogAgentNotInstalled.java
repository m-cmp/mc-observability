package com.mcmp.o11ymanager.manager.exception.agent;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;

public class LogAgentNotInstalled extends BaseException {

    public LogAgentNotInstalled(String requestId, String hostId) {
        super(
                requestId,
                ErrorCode.LOG_AGENT_NOT_EXIST,
                String.format("No agent found on host (ID: %s).", hostId));
    }
}
