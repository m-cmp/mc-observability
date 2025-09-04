package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.entity.TargetEntity;
import com.mcmp.o11ymanager.manager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.manager.service.domain.TargetDomainService;
import com.mcmp.o11ymanager.manager.service.interfaces.StatusService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatusServiceImpl implements StatusService {
    private static final Lock agentTaskStatusLock = new ReentrantLock();
    private final TargetDomainService targetDomainService;

    @Override
    public void updateTargetAgentTaskStatus(
            String requestId,
            Integer taskId,
            TargetAgentTaskStatus hostAgentTaskStatus,
            String nsId,
            String mciId,
            String targetId) {
        log.debug(
                "Updating Agent Task Status - Request ID: {}, Target: {}/{}/{}, Agent Task Status: {}",
                requestId,
                nsId,
                mciId,
                targetId,
                hostAgentTaskStatus);

        TargetEntity updateTarget =
                TargetEntity.builder().nsId(nsId).mciId(mciId).targetId(targetId).build();

        updateTarget.setMonitoringAgentTaskStatus(hostAgentTaskStatus);
        updateTarget.setLogAgentTaskStatus(hostAgentTaskStatus);

        if (hostAgentTaskStatus == TargetAgentTaskStatus.IDLE) {
            updateTarget.setTargetMonitoringAgentTaskId("");
            updateTarget.setTargetLogAgentTaskId("");
        } else if (taskId != null) {
            updateTarget.setTargetMonitoringAgentTaskId(taskId.toString());
            updateTarget.setTargetLogAgentTaskId(taskId.toString());
        }

        targetDomainService.updateTarget(updateTarget);
    }

    @Override
    public void resetTargetAgentTaskStatus(
            String requestId, String nsId, String mciId, String targetId) {
        if (nsId == null
                || nsId.isEmpty()
                || mciId == null
                || mciId.isEmpty()
                || targetId == null
                || targetId.isEmpty()) {
            return;
        }

        try {
            agentTaskStatusLock.lock();
            updateTargetAgentTaskStatus(
                    requestId, null, TargetAgentTaskStatus.IDLE, nsId, mciId, targetId);
        } finally {
            agentTaskStatusLock.unlock();
        }
    }
}
