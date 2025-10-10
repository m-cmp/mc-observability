package com.mcmp.o11ymanager.manager.exception.agent;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;

public class MonitoringAgentNotInstalled extends BaseException {

    public MonitoringAgentNotInstalled(String requestId, String hostId) {
        super(
                requestId,
                ErrorCode.MONITORING_AGENT_NOT_EXIST,
                String.format("No agent exists on host (ID: %s).", hostId));
    }
}
