package com.mcmp.o11ymanager.manager.facade;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.semaphore.Project;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.port.SemaphorePort;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SchedulerFacadeServiceTest {

    @Mock private SemaphorePort semaphorePort;
    @Mock private VMService vmService;

    @InjectMocks private SchedulerFacadeService schedulerFacadeService;

    private static final String NS_ID = "ns-1";
    private static final String MCI_ID = "mci-1";
    private static final String VM_ID = "vm-1";
    private static final String REQUEST_ID = "req-test";
    private static final Integer TASK_ID = 42;
    private static final String PROJECT_NAME = "test-project";

    private Project mockProject;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(schedulerFacadeService, "projectName", PROJECT_NAME);
        ReflectionTestUtils.setField(schedulerFacadeService, "checkIntervalSec", 1);
        ReflectionTestUtils.setField(schedulerFacadeService, "maxWaitMinutes", 1);

        mockProject = Project.builder().id(1).name(PROJECT_NAME).build();
    }

    @Test
    @DisplayName("success 상태 -> FINISHED 업데이트 후 스케줄 취소")
    void successStatus_updatesToFinished() throws InterruptedException {
        Task successTask = Task.builder().id(TASK_ID).status("success").build();
        when(semaphorePort.getProjectByName(PROJECT_NAME)).thenReturn(mockProject);
        when(semaphorePort.getTask(mockProject.getId(), TASK_ID)).thenReturn(successTask);

        schedulerFacadeService.scheduleTaskStatusCheck(
                REQUEST_ID,
                TASK_ID,
                NS_ID,
                MCI_ID,
                VM_ID,
                SemaphoreInstallMethod.INSTALL,
                Agent.BEYLA);

        Thread.sleep(3000);

        verify(vmService, atLeastOnce())
                .updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.FINISHED);
    }

    @Test
    @DisplayName("error 상태 -> FAILED 업데이트 후 스케줄 취소")
    void errorStatus_updatesToFailed() throws InterruptedException {
        Task errorTask = Task.builder().id(TASK_ID).status("error").build();
        when(semaphorePort.getProjectByName(PROJECT_NAME)).thenReturn(mockProject);
        when(semaphorePort.getTask(mockProject.getId(), TASK_ID)).thenReturn(errorTask);

        schedulerFacadeService.scheduleTaskStatusCheck(
                REQUEST_ID,
                TASK_ID,
                NS_ID,
                MCI_ID,
                VM_ID,
                SemaphoreInstallMethod.INSTALL,
                Agent.BEYLA);

        Thread.sleep(3000);

        verify(vmService, atLeastOnce())
                .updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.FAILED);
    }

    @Test
    @DisplayName("failed 상태 -> FAILED 업데이트")
    void failedStatus_updatesToFailed() throws InterruptedException {
        Task failedTask = Task.builder().id(TASK_ID).status("failed").build();
        when(semaphorePort.getProjectByName(PROJECT_NAME)).thenReturn(mockProject);
        when(semaphorePort.getTask(mockProject.getId(), TASK_ID)).thenReturn(failedTask);

        schedulerFacadeService.scheduleTaskStatusCheck(
                REQUEST_ID,
                TASK_ID,
                NS_ID,
                MCI_ID,
                VM_ID,
                SemaphoreInstallMethod.INSTALL,
                Agent.BEYLA);

        Thread.sleep(3000);

        verify(vmService, atLeastOnce())
                .updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.FAILED);
    }

    @Test
    @DisplayName("stopped 상태 -> FAILED 업데이트")
    void stoppedStatus_updatesToFailed() throws InterruptedException {
        Task stoppedTask = Task.builder().id(TASK_ID).status("stopped").build();
        when(semaphorePort.getProjectByName(PROJECT_NAME)).thenReturn(mockProject);
        when(semaphorePort.getTask(mockProject.getId(), TASK_ID)).thenReturn(stoppedTask);

        schedulerFacadeService.scheduleTaskStatusCheck(
                REQUEST_ID,
                TASK_ID,
                NS_ID,
                MCI_ID,
                VM_ID,
                SemaphoreInstallMethod.INSTALL,
                Agent.BEYLA);

        Thread.sleep(3000);

        verify(vmService, atLeastOnce())
                .updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.FAILED);
    }

    @Test
    @DisplayName("Telegraf agent -> monitoring 상태 업데이트")
    void telegrafAgent_updatesMonitoringStatus() throws InterruptedException {
        Task successTask = Task.builder().id(TASK_ID).status("success").build();
        when(semaphorePort.getProjectByName(PROJECT_NAME)).thenReturn(mockProject);
        when(semaphorePort.getTask(mockProject.getId(), TASK_ID)).thenReturn(successTask);

        schedulerFacadeService.scheduleTaskStatusCheck(
                REQUEST_ID,
                TASK_ID,
                NS_ID,
                MCI_ID,
                VM_ID,
                SemaphoreInstallMethod.INSTALL,
                Agent.TELEGRAF);

        Thread.sleep(3000);

        verify(vmService, atLeastOnce())
                .updateMonitoringAgentTaskStatus(
                        NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.FINISHED);
    }

    @Test
    @DisplayName("FluentBit agent -> log 상태 업데이트")
    void fluentBitAgent_updatesLogStatus() throws InterruptedException {
        Task successTask = Task.builder().id(TASK_ID).status("success").build();
        when(semaphorePort.getProjectByName(PROJECT_NAME)).thenReturn(mockProject);
        when(semaphorePort.getTask(mockProject.getId(), TASK_ID)).thenReturn(successTask);

        schedulerFacadeService.scheduleTaskStatusCheck(
                REQUEST_ID,
                TASK_ID,
                NS_ID,
                MCI_ID,
                VM_ID,
                SemaphoreInstallMethod.INSTALL,
                Agent.FLUENT_BIT);

        Thread.sleep(3000);

        verify(vmService, atLeastOnce())
                .updateLogAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.FINISHED);
    }

    @Test
    @DisplayName("예외 발생 시 스케줄 취소하지 않고 계속 폴링")
    void exceptionDuringPolling_continuesPolling() throws InterruptedException {
        when(semaphorePort.getProjectByName(PROJECT_NAME))
                .thenThrow(new RuntimeException("connection error"))
                .thenThrow(new RuntimeException("connection error"));

        schedulerFacadeService.scheduleTaskStatusCheck(
                REQUEST_ID,
                TASK_ID,
                NS_ID,
                MCI_ID,
                VM_ID,
                SemaphoreInstallMethod.INSTALL,
                Agent.BEYLA);

        Thread.sleep(3000);

        verify(semaphorePort, atLeastOnce()).getProjectByName(PROJECT_NAME);
        verify(vmService, never()).updateTraceAgentTaskStatus(anyString(), anyString(), anyString(), eq(VMAgentTaskStatus.FINISHED));
        verify(vmService, never()).updateTraceAgentTaskStatus(anyString(), anyString(), anyString(), eq(VMAgentTaskStatus.FAILED));
    }
}
