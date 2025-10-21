package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.service.domain.VMDomainService;
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
    private final VMDomainService vmDomainService;

    @Override
    public void updateVMAgentTaskStatus(
            String requestId,
            Integer taskId,
            VMAgentTaskStatus hostAgentTaskStatus,
            String nsId,
            String mciId,
            String vmId) {
        log.debug(
                "Updating Agent Task Status - Request ID: {}, VM: {}/{}/{}, Agent Task Status: {}",
                requestId,
                nsId,
                mciId,
                vmId,
                hostAgentTaskStatus);

        VMEntity updateVM = VMEntity.builder().nsId(nsId).mciId(mciId).vmId(vmId).build();

        updateVM.setMonitoringAgentTaskStatus(hostAgentTaskStatus);
        updateVM.setLogAgentTaskStatus(hostAgentTaskStatus);

        if (hostAgentTaskStatus == VMAgentTaskStatus.IDLE) {
            updateVM.setVmMonitoringAgentTaskId("");
            updateVM.setVmLogAgentTaskId("");
        } else if (taskId != null) {
            updateVM.setVmMonitoringAgentTaskId(taskId.toString());
            updateVM.setVmLogAgentTaskId(taskId.toString());
        }

        vmDomainService.updateVM(updateVM);
    }

    @Override
    public void resetVMAgentTaskStatus(String requestId, String nsId, String mciId, String vmId) {
        if (nsId == null
                || nsId.isEmpty()
                || mciId == null
                || mciId.isEmpty()
                || vmId == null
                || vmId.isEmpty()) {
            return;
        }

        try {
            agentTaskStatusLock.lock();
            updateVMAgentTaskStatus(requestId, null, VMAgentTaskStatus.IDLE, nsId, mciId, vmId);
        } finally {
            agentTaskStatusLock.unlock();
        }
    }
}
