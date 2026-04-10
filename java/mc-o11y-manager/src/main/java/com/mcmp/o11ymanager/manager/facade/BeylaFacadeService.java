package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator.BeylaSystemCheckResult;
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
public class BeylaFacadeService {

    private final VMService vmService;
    private static final Lock agentTaskStatusLock = new ReentrantLock();
    private final RequestInfo requestInfo;
    private final SemaphoreDomainService semaphoreDomainService;
    private final SchedulerFacadeService schedulerFacadeService;
    private final TumblebugService tumblebugService;
    private final BeylaSystemRequirementValidator beylaSystemRequirementValidator;

    public void install(
            String nsId,
            String mciId,
            String vmId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        log.info("==========================beyla idle start===============================");
        vmService.isIdleTraceAgent(nsId, mciId, vmId);
        log.info("==========================beyla idle finish===============================");

        log.info("==========================beyla system requirement check start===============================");
        beylaSystemRequirementValidator.validateAndThrow(nsId, mciId, vmId);
        // 해당 vm에서 Beyla를 설치하기 위해 커널 버전 및 BTF 파일 존재 유뮤 확인
        log.info("==========================beyla system requirement check finish===============================");

        vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.INSTALLING);
        // 특정 VM의 작업 상태를 DB에 업데이트 (작업 생태를 INSTALLING으로 변환)
        log.info("==========================update vm status===============================");

        log.info("========================= START BEYLA INSTALL REQUEST============================");

        Task task;
        try {
            task =
                    semaphoreDomainService.install(
                            accessInfo,
                            SemaphoreInstallMethod.INSTALL,
                            null,
                            Agent.BEYLA,
                            templateCount);
        } catch (Exception e) {
            // Semaphore 호출 실패 시 상태를 IDLE로 되돌려 재시도가 가능하도록 함
            vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
            throw e;
        }

        log.info("=========================FINISH BEYLA INSTALL REQUEST============================");

        vmService.updateTraceAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.INSTALLING, String.valueOf(task.getId()));

        log.info(
                "==========================update vm status task ID: {}============================",
                task.getId());

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),               // 로깅용 추적 ID를 여기서 넣어주는 이유는 @RequestScope인 RequestInfo는 스케줄러 스레드에서 꺼낼 수 없기 때문입니다. (RequestIdAspect 참고)
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.INSTALL,
                Agent.BEYLA);
    }

    public void update(
            String nsId,
            String mciId,
            String vmId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        vmService.isIdleTraceAgent(nsId, mciId, vmId);

        vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.UPDATING);

        Task task;
        try {
            task =
                    semaphoreDomainService.install(
                            accessInfo,
                            SemaphoreInstallMethod.UPDATE,
                            null,
                            Agent.BEYLA,
                            templateCount);
        } catch (Exception e) {
            // Semaphore 호출 실패 시 상태를 IDLE로 되돌려 재시도가 가능하도록 함
            vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
            throw e;
        }

        vmService.updateTraceAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.UPDATING, String.valueOf(task.getId()));

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.UPDATE,
                Agent.BEYLA);
    }

    public void uninstall(
            String nsId, String mciId, String vmId, AccessInfoDTO accessInfo, int templateCount) {

        vmService.isIdleTraceAgent(nsId, mciId, vmId);

        vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.PREPARING);

        Task task;
        try {
            task =
                    semaphoreDomainService.install(
                            accessInfo,
                            SemaphoreInstallMethod.UNINSTALL,
                            null,
                            Agent.BEYLA,
                            templateCount);
        } catch (Exception e) {
            // Semaphore 호출 실패 시 상태를 IDLE로 되돌려 재시도가 가능하도록 함
            vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
            throw e;
        }

        vmService.updateTraceAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.UNINSTALLING, String.valueOf(task.getId()));

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.UNINSTALL,
                Agent.BEYLA);
    }

    @Transactional
    public List<ResultDTO> restart(String nsId, String mciId, String vmId) {

        List<ResultDTO> results = new ArrayList<>();

        try {
            agentTaskStatusLock.lock();

            vmService.isIdleTraceAgent(nsId, mciId, vmId);

            vmService.updateTraceAgentTaskStatus(
                    nsId, mciId, vmId, VMAgentTaskStatus.RESTARTING);

            tumblebugService.restart(nsId, mciId, vmId, Agent.BEYLA);

            vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);

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

            vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);

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
