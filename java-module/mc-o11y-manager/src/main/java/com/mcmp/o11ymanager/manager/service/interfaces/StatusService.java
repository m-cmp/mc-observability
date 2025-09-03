package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.model.host.TargetAgentTaskStatus;

public interface StatusService {

    void updateTargetAgentTaskStatus(
            String requestId,
            Integer taskId,
            TargetAgentTaskStatus hostAgentTaskStatus,
            String nsId,
            String mciId,
            String targetId);

    void resetTargetAgentTaskStatus(String requestId, String nsId, String mciId, String targetId);
}
