package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;

public interface StatusService {

    void updateVMAgentTaskStatus(
            String requestId,
            Integer taskId,
            VMAgentTaskStatus hostAgentTaskStatus,
            String nsId,
            String mciId,
            String vmId);

    void resetVMAgentTaskStatus(String requestId, String nsId, String mciId, String vmId);
}
