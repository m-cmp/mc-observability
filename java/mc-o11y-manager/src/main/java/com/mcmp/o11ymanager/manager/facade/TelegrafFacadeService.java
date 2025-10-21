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
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
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
@Service
@RequiredArgsConstructor
public class TelegrafFacadeService {

    private final VMService vmService;
    private static final Lock agentTaskStatusLock = new ReentrantLock();
    private final RequestInfo requestInfo;
    private final SemaphoreDomainService semaphoreDomainService;
    private final SchedulerFacadeService schedulerFacadeService;
    private final TelegrafConfigFacadeService telegrafConfigFacadeService;
    private final TumblebugService tumblebugService;

    public void install(
            String nsId,
            String mciId,
            String vmId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        log.info("==========================telegraf idle start===============================");
        vmService.isIdleMonitoringAgent(nsId, mciId, vmId);
        log.info("==========================telegraf idle finish===============================");

        vmService.updateMonitoringAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.INSTALLING);
        log.info("==========================update vm status===============================");

        String configContent = telegrafConfigFacadeService.initTelegrafConfig(nsId, mciId, vmId);

        log.info("Telegraf config: {}", configContent);

        log.info("========================= START INSTALL REQUEST============================");

        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.INSTALL,
                        configContent,
                        Agent.TELEGRAF,
                        templateCount);

        log.info("=========================FINISH INSTALL REQUEST============================");

        vmService.updateMonitoringAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.INSTALLING, String.valueOf(task.getId()));

        log.info(
                "==========================update vm status task ID: {}============================",
                task.getId());

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.INSTALL,
                Agent.TELEGRAF);
    }

    public void update(
            String nsId,
            String mciId,
            String vmId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        vmService.isIdleMonitoringAgent(nsId, mciId, vmId);

        vmService.updateMonitoringAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.UPDATING);

        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.UPDATE,
                        null,
                        Agent.TELEGRAF,
                        templateCount);

        vmService.updateMonitoringAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.UPDATING, String.valueOf(task.getId()));

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.UPDATE,
                Agent.TELEGRAF);
    }

    public void uninstall(
            String nsId, String mciId, String vmId, AccessInfoDTO accessInfo, int templateCount) {

        vmService.isIdleMonitoringAgent(nsId, mciId, vmId);

        vmService.updateMonitoringAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.PREPARING);

        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.UNINSTALL,
                        null,
                        Agent.TELEGRAF,
                        templateCount);

        vmService.updateMonitoringAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.UNINSTALLING, String.valueOf(task.getId()));

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.UNINSTALL,
                Agent.TELEGRAF);
    }

    @Transactional
    public List<ResultDTO> restart(String nsId, String mciId, String vmId) {

        List<ResultDTO> results = new ArrayList<>();

        try {
            agentTaskStatusLock.lock();

            vmService.isIdleMonitoringAgent(nsId, mciId, vmId);

            vmService.updateMonitoringAgentTaskStatus(
                    nsId, mciId, vmId, VMAgentTaskStatus.RESTARTING);

            tumblebugService.restart(nsId, mciId, vmId, Agent.TELEGRAF);

            vmService.updateMonitoringAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);

            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .mciId(mciId)
                            .vmId(vmId)
                            .status(ResponseStatus.SUCCESS)
                            .build());
            agentTaskStatusLock.unlock();
        } catch (Exception e) {
            agentTaskStatusLock.unlock();

            vmService.updateMonitoringAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);

            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .mciId(mciId)
                            .vmId(vmId)
                            .status(ResponseStatus.ERROR)
                            .errorMessage(e.getMessage())
                            .build());
        }

        return results;
    }
}
