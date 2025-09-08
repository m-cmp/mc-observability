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
            String mciId,
            String vmId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        // 1. host IDLE 상태 확인
        vmService.isIdleLogAgent(nsId, mciId, vmId);

        // 2. host 상태 업데이트
        vmService.updateLogAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.INSTALLING);

        String configContent = fluentBitConfigFacadeService.initFluentbitConfig(nsId, mciId, vmId);

        log.info(String.format("Fluent-Bit config: %s", configContent));

        // 4. 전송(semaphore) - 설치 요청
        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.INSTALL,
                        configContent,
                        Agent.FLUENT_BIT,
                        templateCount);

        // 5. task ID, task status 업데이트
        vmService.updateLogAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.INSTALLING, String.valueOf(task.getId()));

        // 7. 스케줄러 등록
        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.INSTALL,
                Agent.FLUENT_BIT);
    }

    public void update(
            String nsId,
            String mciId,
            String vmId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        // 1. host IDLE 상태 확인
        vmService.isIdleLogAgent(nsId, mciId, vmId);

        // 2. host 상태 업데이트
        vmService.updateLogAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.UPDATING);

        // 3. 전송(semaphore) - 업데이트 요청
        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.UPDATE,
                        null,
                        Agent.FLUENT_BIT,
                        templateCount);

        // 4. task ID, task status 업데이트
        vmService.updateLogAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.UPDATING, String.valueOf(task.getId()));

        // 6. 스케줄러 등록
        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.UPDATE,
                Agent.FLUENT_BIT);
    }

    public void uninstall(
            String nsId, String mciId, String vmId, AccessInfoDTO accessInfo, int templateCount) {

        // 1) 상태 확인
        vmService.isIdleLogAgent(nsId, mciId, vmId);

        // 2) 상태 변경
        vmService.updateLogAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.PREPARING);

        // 3. 전송(semaphore) - 삭제 요청
        Task task =
                semaphoreDomainService.install(
                        accessInfo,
                        SemaphoreInstallMethod.UNINSTALL,
                        null,
                        Agent.FLUENT_BIT,
                        templateCount);

        // 5. task ID, task status 업데이트
        vmService.updateLogAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.UNINSTALLING, String.valueOf(task.getId()));

        // 6) 스케줄러 등록
        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.UNINSTALL,
                Agent.FLUENT_BIT);
    }

    @Transactional
    public List<ResultDTO> restart(String nsId, String mciId, String vmId) {
        List<ResultDTO> results = new ArrayList<>();

        try {
            agentTaskStatusLock.lock();

            // 1. 싫행 상태 확인
            vmService.isIdleLogAgent(nsId, mciId, vmId);

            // 2. RESTARTING 상태로 변경
            vmService.updateLogAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.RESTARTING);

            // TODO : Use Tumblebug CMD - 3. restart 실행

            vmService.updateLogAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);

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

            vmService.updateLogAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);

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
