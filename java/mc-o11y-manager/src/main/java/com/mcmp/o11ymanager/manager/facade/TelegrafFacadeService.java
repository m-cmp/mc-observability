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
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {
        install(nsId, infraId, nodeId, accessInfo, templateCount, false);
    }

    // gpu: true면 telegraf config에 GPU(DCGM) 수집 블록을 포함하고,
    // Ansible enable_gpu 변수로 DCGM Exporter 설치까지 함께 수행한다.
    public void install(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount,
            boolean gpu)
            throws Exception {

        log.info("==========================telegraf idle start===============================");
        vmService.isIdleMonitoringAgent(nsId, infraId, nodeId);
        log.info("==========================telegraf idle finish===============================");

        vmService.updateMonitoringAgentTaskStatus(
                nsId, infraId, nodeId, VMAgentTaskStatus.INSTALLING);
        log.info("==========================update vm status===============================");

        String configContent =
                telegrafConfigFacadeService.initTelegrafConfig(nsId, infraId, nodeId, gpu);

        log.info("Telegraf config: {}", configContent);

        log.info("========================= START INSTALL REQUEST============================");

        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.INSTALL,
                        configContent,
                        Agent.TELEGRAF,
                        templateCount,
                        gpu);

        log.info("=========================FINISH INSTALL REQUEST============================");

        vmService.updateMonitoringAgentTaskStatusAndTaskId(
                nsId, infraId, nodeId, VMAgentTaskStatus.INSTALLING, String.valueOf(task.getId()));

        log.info(
                "==========================update vm status task ID: {}============================",
                task.getId());

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                infraId,
                nodeId,
                SemaphoreInstallMethod.INSTALL,
                Agent.TELEGRAF);
    }

    public void update(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        vmService.isIdleMonitoringAgent(nsId, infraId, nodeId);

        vmService.updateMonitoringAgentTaskStatus(
                nsId, infraId, nodeId, VMAgentTaskStatus.UPDATING);

        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.UPDATE,
                        null,
                        Agent.TELEGRAF,
                        templateCount);

        vmService.updateMonitoringAgentTaskStatusAndTaskId(
                nsId, infraId, nodeId, VMAgentTaskStatus.UPDATING, String.valueOf(task.getId()));

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                infraId,
                nodeId,
                SemaphoreInstallMethod.UPDATE,
                Agent.TELEGRAF);
    }

    public void uninstall(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            int templateCount) {

        vmService.isIdleMonitoringAgent(nsId, infraId, nodeId);

        vmService.updateMonitoringAgentTaskStatus(
                nsId, infraId, nodeId, VMAgentTaskStatus.PREPARING);

        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.UNINSTALL,
                        null,
                        Agent.TELEGRAF,
                        templateCount);

        vmService.updateMonitoringAgentTaskStatusAndTaskId(
                nsId,
                infraId,
                nodeId,
                VMAgentTaskStatus.UNINSTALLING,
                String.valueOf(task.getId()));

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                infraId,
                nodeId,
                SemaphoreInstallMethod.UNINSTALL,
                Agent.TELEGRAF);
    }

    @Transactional
    public List<ResultDTO> restart(String nsId, String infraId, String nodeId) {

        List<ResultDTO> results = new ArrayList<>();

        try {
            agentTaskStatusLock.lock();

            vmService.isIdleMonitoringAgent(nsId, infraId, nodeId);

            vmService.updateMonitoringAgentTaskStatus(
                    nsId, infraId, nodeId, VMAgentTaskStatus.RESTARTING);

            tumblebugService.restart(nsId, infraId, nodeId, Agent.TELEGRAF);

            vmService.updateMonitoringAgentTaskStatus(
                    nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);

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

            vmService.updateMonitoringAgentTaskStatus(
                    nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);

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
