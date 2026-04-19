package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.semaphore.Project;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.port.SemaphorePort;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SchedulerFacadeService {

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(10); // 동시에 풀링 가능한 태스트 최대 10개

    @PostConstruct
    void debugSchedulerBean() {
        log.info(
                "==================================================[SchedulerFacadeService] injected scheduler bean={}, id={}==================================================",
                scheduler.getClass().getName(),
                System.identityHashCode(scheduler));

        scheduler.schedule(
                () ->
                        log.info(
                                "==================================================[SchedulerFacadeService] scheduler thread={}==================================================",
                                Thread.currentThread().getName()),
                0,
                TimeUnit.SECONDS);
    }

    private final SemaphorePort semaphorePort;
    private final VMService vmService;

    @Value("${feign.semaphore.project-name}")
    private String projectName;

    @Value("${feign.semaphore.task-check-scheduler.check-interval-sec:5}")
    private int checkIntervalSec;

    @Value("${feign.semaphore.task-check-scheduler.max-wait-minutes:30}")
    private int maxWaitMinutes;

    // Semaphore에 큐잉된 태스크의 완료, 실패, 타임아웃을 주기적으로 pulling 해서 VM의 에이전트 작업 상태를 DB에 반영하는 백그라운드 감시자
    public void scheduleTaskStatusCheck(
            String requestId,
            Integer taskId,
            String nsId,
            String mciId,
            String vmId,
            SemaphoreInstallMethod method,
            Agent agent) {

        AtomicLong startTime = new AtomicLong(System.currentTimeMillis()); // 타임아웃 측정 기준점
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
        // 람다 내부에서 자기 자신(ScheduledFuture)을 cancel()하기 위한 self-reference 트릭.
        // 람다가 정의될 때는 아직 future가 없으므로 AtomicReference로 forward-declaration.

        ScheduledFuture<?> future =
                scheduler.scheduleAtFixedRate(
                        () -> {
                            // 풀링 로직
                            try {
                                long currentTime = System.currentTimeMillis();

                                Project project =
                                        semaphorePort.getProjectByName(
                                                projectName); // Ansible Semaphore API로 프로젝트 호출 후
                                // projectName과 같은 프로젝트를 찾음
                                Task currentTask = semaphorePort.getTask(project.getId(), taskId);
                                String status =
                                        Optional.ofNullable(currentTask.getStatus())
                                                .orElse("")
                                                .toLowerCase();

                                log.debug(
                                        "Task Status - Request ID: {}, VM: {}/{}/{}, Agent: {}, Method: {}, Task ID: {}, Status: {}",
                                        requestId,
                                        nsId,
                                        mciId,
                                        vmId,
                                        agent,
                                        method,
                                        currentTask.getId(),
                                        status);

                                // waiting: reset start time and keep checking
                                // Semaphore 큐에서 대기 중. startTime을 현재 시각으로 리셋 -> 타임아웃 카운트 연기. 즉, 태스크가
                                // 실제로 시작되지 않은 동안은 타임아웃 안잡힘
                                if ("waiting".equals(status)) {
                                    startTime.set(System.currentTimeMillis());
                                    return;
                                }

                                // timeout
                                // 에이전트 별로 IDLE 상태로 되돌림 (원상복구), 스케줄 취소 -> 루프 종료
                                if (currentTime - startTime.get()
                                        > TimeUnit.MINUTES.toMillis(maxWaitMinutes)) {
                                    log.debug(
                                            "Task timed out after {} minutes. Resetting to IDLE. VM: {}/{}/{}, Agent: {}",
                                            maxWaitMinutes,
                                            nsId,
                                            mciId,
                                            vmId,
                                            agent);

                                    if (agent == Agent.TELEGRAF) {
                                        vmService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        vmService.updateLogAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
                                    } else if (agent == Agent.BEYLA) {
                                        vmService.updateTraceAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.IDLE);
                                    }

                                    log.warn("Timeout occurred for agent {}", agent);
                                    ScheduledFuture<?> scheduledFuture = futureRef.get();
                                    if (scheduledFuture != null) {

                                        scheduledFuture.cancel(false);
                                    }
                                    return;
                                }

                                // success case
                                // 에이전트 별, FINISHED 상태로 DB 업데이트
                                if ("success".equals(status)) {
                                    log.debug("Task successful for agent {}", agent);
                                    if (agent == Agent.TELEGRAF) {
                                        vmService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FINISHED);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        vmService.updateLogAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FINISHED);
                                    } else if (agent == Agent.BEYLA) {
                                        vmService.updateTraceAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FINISHED);
                                    }

                                    ScheduledFuture<?> scheduledFuture = futureRef.get();
                                    if (scheduledFuture != null) {
                                        scheduledFuture.cancel(false);
                                    }
                                    return;
                                }

                                // failed case
                                if ("error".equals(status)
                                        || "failed".equals(status)
                                        || "stopped".equals(status)) {
                                    log.debug("Task failed for agent {}", agent);

                                    if (agent == Agent.TELEGRAF) {
                                        vmService.updateMonitoringAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FAILED);
                                    } else if (agent == Agent.FLUENT_BIT) {
                                        vmService.updateLogAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FAILED);
                                    } else if (agent == Agent.BEYLA) {
                                        vmService.updateTraceAgentTaskStatus(
                                                nsId, mciId, vmId, VMAgentTaskStatus.FAILED);
                                    }

                                    ScheduledFuture<?> scheduledFuture = futureRef.get();
                                    if (scheduledFuture != null) {
                                        scheduledFuture.cancel(false);
                                    }
                                    return;
                                }

                                // running or other statuses: continue checking (다음 주기 까지 대기)
                            } catch (Exception e) {
                                // Do NOT cancel future on exception, only log
                                log.error(
                                        "Error while checking task status for agent {}: {}",
                                        agent,
                                        e.getMessage());
                            }
                        },
                        0, // initialDelay : 즉시 첫 실행
                        checkIntervalSec, // 주기
                        TimeUnit.SECONDS);

        futureRef.set(future);
    }
}
