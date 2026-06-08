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
    private final BeylaConfigFacadeService beylaConfigFacadeService;

    public void install(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        log.info("==========================beyla idle start===============================");
        vmService.isIdleTraceAgent(nsId, infraId, nodeId);
        log.info("==========================beyla idle finish===============================");

        log.info(
                "==========================beyla system requirement check start===============================");
        beylaSystemRequirementValidator.validateAndThrow(nsId, infraId, nodeId);
        // 해당 vm에서 Beyla를 설치하기 위해 커널 버전 및 BTF 파일 존재 유뮤 확인
        log.info(
                "==========================beyla system requirement check finish===============================");

        vmService.updateTraceAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.INSTALLING);
        // 특정 VM의 작업 상태를 DB에 업데이트 (작업 생태를 INSTALLING으로 변환)
        log.info("==========================update vm status===============================");

        log.info(
                "========================= START BEYLA INSTALL REQUEST============================");

        // BeylaConfigFacadeService에서 ClassPath의 beyla_template.yaml + application.yaml의
        // beyla.otel-endpoint를 합쳐 최종 yaml 생성. install 직후 chained config-update가
        // 이 conf로 원격의 정적 placeholder yaml을 덮어씀.
        String configContent = beylaConfigFacadeService.initBeylaConfig(nsId, infraId, nodeId);
        log.info("Beyla config: {}", configContent);

        Task task;
        try {
            task =
                    semaphoreDomainService.install(
                            accessInfo,
                            SemaphoreInstallMethod.INSTALL,
                            configContent,
                            Agent.BEYLA,
                            templateCount);
        } catch (Exception e) {
            // Semaphore 호출 실패 시 상태를 IDLE로 되돌려 재시도가 가능하도록 함
            vmService.updateTraceAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);
            throw e;
        }

        log.info(
                "=========================FINISH BEYLA INSTALL REQUEST============================");

        vmService.updateTraceAgentTaskStatusAndTaskId(
                nsId, infraId, nodeId, VMAgentTaskStatus.INSTALLING, String.valueOf(task.getId()));

        log.info(
                "==========================update vm status task ID: {}============================",
                task.getId());

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo
                        .getRequestId(), // 로깅용 추적 ID를 여기서 넣어주는 이유는 @RequestScope인 RequestInfo는 스케줄러
                // 스레드에서 꺼낼 수 없기 때문입니다. (RequestIdAspect 참고)
                task.getId(),
                nsId,
                infraId,
                nodeId,
                SemaphoreInstallMethod.INSTALL,
                Agent.BEYLA);
    }

    public void update(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        vmService.isIdleTraceAgent(nsId, infraId, nodeId);

        vmService.updateTraceAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.UPDATING);

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
            vmService.updateTraceAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);
            throw e;
        }

        vmService.updateTraceAgentTaskStatusAndTaskId(
                nsId, infraId, nodeId, VMAgentTaskStatus.UPDATING, String.valueOf(task.getId()));

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                infraId,
                nodeId,
                SemaphoreInstallMethod.UPDATE,
                Agent.BEYLA);
    }

    public void uninstall(
            String nsId,
            String infraId,
            String nodeId,
            AccessInfoDTO accessInfo,
            int templateCount) {

        vmService.isIdleTraceAgent(nsId, infraId, nodeId);

        vmService.updateTraceAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.PREPARING);

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
            vmService.updateTraceAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);
            throw e;
        }

        vmService.updateTraceAgentTaskStatusAndTaskId(
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
                Agent.BEYLA);
    }

    @Transactional
    public List<ResultDTO> restart(String nsId, String infraId, String nodeId) {

        List<ResultDTO> results = new ArrayList<>();

        try {
            agentTaskStatusLock.lock();

            vmService.isIdleTraceAgent(nsId, infraId, nodeId);

            vmService.updateTraceAgentTaskStatus(
                    nsId, infraId, nodeId, VMAgentTaskStatus.RESTARTING);

            tumblebugService.restart(nsId, infraId, nodeId, Agent.BEYLA);

            vmService.updateTraceAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);

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

            vmService.updateTraceAgentTaskStatus(nsId, infraId, nodeId, VMAgentTaskStatus.IDLE);

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
