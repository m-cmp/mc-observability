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

/**
 * Windows VM 대상 OpenTelemetry Java Auto-Instrumentation 설치/업데이트/제거/재시작 담당 Facade.
 *
 * <p>{@link BeylaFacadeService}를 본뜬 구조로, 동일한 trace agent 상태 컬럼 ({@code
 * vmTraceAgentTaskStatus}/{@code vmTraceAgentTaskId})을 공유한다. Beyla와 다른 점:
 *
 * <ul>
 *   <li>System requirement validator 미사용 (BTF/커널 체크는 Linux 한정 — POC에선 OS=Windows 분기 자체로 충분).
 *   <li>config payload는 yaml 대신 OTel KEY=VALUE 환경변수 묶음 ({@link OtelJavaConfigFacadeService}).
 *   <li>{@code Agent.OTEL_JAVA_AGENT}로 Semaphore에 큐잉되어, Ansible playbook은 이를 보고 Windows 호스트 대상 처리
 *       (jar 다운로드 + setx /M 환경변수 set).
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtelJavaFacadeService {

    private final VMService vmService;
    private static final Lock agentTaskStatusLock = new ReentrantLock();
    private final RequestInfo requestInfo;
    private final SemaphoreDomainService semaphoreDomainService;
    private final SchedulerFacadeService schedulerFacadeService;
    private final TumblebugService tumblebugService;
    private final OtelJavaConfigFacadeService otelJavaConfigFacadeService;

    public void install(
            String nsId,
            String mciId,
            String vmId,
            AccessInfoDTO accessInfo,
            @NotBlank int templateCount)
            throws Exception {

        vmService.isIdleTraceAgent(nsId, mciId, vmId);

        vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.INSTALLING);

        String configContent = otelJavaConfigFacadeService.initOtelJavaConfig(nsId, mciId, vmId);
        log.info("OTel Java config: {}", configContent);

        Task task;
        try {
            task =
                    semaphoreDomainService.install(
                            accessInfo,
                            SemaphoreInstallMethod.INSTALL,
                            configContent,
                            Agent.OTEL_JAVA_AGENT,
                            templateCount);
        } catch (Exception e) {
            vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
            throw e;
        }

        vmService.updateTraceAgentTaskStatusAndTaskId(
                nsId, mciId, vmId, VMAgentTaskStatus.INSTALLING, String.valueOf(task.getId()));

        schedulerFacadeService.scheduleTaskStatusCheck(
                requestInfo.getRequestId(),
                task.getId(),
                nsId,
                mciId,
                vmId,
                SemaphoreInstallMethod.INSTALL,
                Agent.OTEL_JAVA_AGENT);
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
                            Agent.OTEL_JAVA_AGENT,
                            templateCount);
        } catch (Exception e) {
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
                Agent.OTEL_JAVA_AGENT);
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
                            Agent.OTEL_JAVA_AGENT,
                            templateCount);
        } catch (Exception e) {
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
                Agent.OTEL_JAVA_AGENT);
    }

    @Transactional
    public List<ResultDTO> restart(String nsId, String mciId, String vmId) {

        List<ResultDTO> results = new ArrayList<>();

        try {
            agentTaskStatusLock.lock();

            vmService.isIdleTraceAgent(nsId, mciId, vmId);

            vmService.updateTraceAgentTaskStatus(nsId, mciId, vmId, VMAgentTaskStatus.RESTARTING);

            tumblebugService.restart(nsId, mciId, vmId, Agent.OTEL_JAVA_AGENT);

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
