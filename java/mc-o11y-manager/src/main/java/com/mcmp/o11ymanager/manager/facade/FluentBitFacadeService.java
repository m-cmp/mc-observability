package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.service.domain.SemaphoreDomainService;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class FluentBitFacadeService {

    private final VMService vmService;
    private static final Lock agentTaskStatusLock = new ReentrantLock();
    private final RequestInfo requestInfo;
    private final SemaphoreDomainService semaphoreDomainService;
    private final SchedulerFacadeService schedulerFacadeService;
    private final FluentBitConfigFacadeService fluentBitConfigFacadeService;

    public void install(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        // 1. Check if host is in IDLE state
        vmService.isIdleLogAgent(nsId, infraId, nodeId);

        // 2. Update host status
        vmService.updateLogAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.INSTALLING);

        String configContent =
                fluentBitConfigFacadeService.initFluentbitConfig(nsId, infraId, nodeId);

        log.info(String.format("Fluent-Bit config: %s", configContent));

        // 4. Send (via semaphore) - installation request
        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.INSTALL,
                        configContent,
                        Agent.FLUENT_BIT,
                        templateCount);

        // 5. Update task ID and task status
        vmService.updateLogAgentTaskStatusAndTaskId(
                nsId, infraId, nodeId, VMAgentTaskStatus.INSTALLING, String.valueOf(task.getId()));

        // 7. Register scheduler
        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                infraId,
                nodeId,
                SemaphoreInstallMethod.INSTALL,
                Agent.FLUENT_BIT);
    }

    public void update(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        // 1. Check if host is in IDLE state
        vmService.isIdleLogAgent(nsId, infraId, nodeId);

        // 2. Update host status
        vmService.updateLogAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.UPDATING);

        // 3. Send (via semaphore) - update request
        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.UPDATE,
                        null,
                        Agent.FLUENT_BIT,
                        templateCount);

        // 4. Update task ID and task status
        vmService.updateLogAgentTaskStatusAndTaskId(
                nsId, infraId, nodeId, VMAgentTaskStatus.UPDATING, String.valueOf(task.getId()));

        // 6. Register scheduler
        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                infraId,
                nodeId,
                SemaphoreInstallMethod.UPDATE,
                Agent.FLUENT_BIT);
    }

    public void uninstall(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            int templateCount) {

        // 1) Check current status
        vmService.isIdleLogAgent(nsId, infraId, nodeId);

        // 2) Update status
        vmService.updateLogAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.PREPARING);

        // 3. Send (via semaphore) - uninstall request
        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.UNINSTALL,
                        null,
                        Agent.FLUENT_BIT,
                        templateCount);

        // 5. Update task ID and task status
        vmService.updateLogAgentTaskStatusAndTaskId(
                nsId,
                infraId,
                nodeId,
                VMAgentTaskStatus.UNINSTALLING,
                String.valueOf(task.getId()));

        // 6) Register scheduler
        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                infraId,
                nodeId,
                SemaphoreInstallMethod.UNINSTALL,
                Agent.FLUENT_BIT);
    }

    @Transactional
    public List<ResultDTO> restart(String nsId, String infraId, String nodeId) {
        List<ResultDTO> results = new ArrayList<>();

        try {
            agentTaskStatusLock.lock();

            // 1. Check if host is in IDLE state
            vmService.isIdleLogAgent(nsId, infraId, nodeId);

            // 2. Change status to RESTARTING
            vmService.updateLogAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.RESTARTING);

            // TODO: Use Tumblebug CMD - 3. Execute restart

            vmService.updateLogAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);

            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .infraId(infraId)
                            .nodeId(nodeId)
                            .status(ResponseStatus.SUCCESS)
                            .build());
            agentTaskStatusLock.unlock();
        } catch (Exception e) {
            agentTaskStatusLock.unlock();

            vmService.updateLogAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);

            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .infraId(infraId)
                            .nodeId(nodeId)
                            .status(ResponseStatus.ERROR)
                            .errorMessage(e.getMessage())
                            .build());
        }

        return results;
    }
}
