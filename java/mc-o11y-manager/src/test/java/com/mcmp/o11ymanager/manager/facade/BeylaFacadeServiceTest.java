package com.mcmp.o11ymanager.manager.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.exception.agent.BeylaSystemRequirementException;
import com.mcmp.o11ymanager.manager.exception.vm.VMAgentTaskProcessingException;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator;
import com.mcmp.o11ymanager.manager.service.domain.SemaphoreDomainService;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeylaFacadeServiceTest {

    @Mock private VMService vmService;
    @Mock private RequestInfo requestInfo;
    @Mock private SemaphoreDomainService semaphoreDomainService;
    @Mock private SchedulerFacadeService schedulerFacadeService;
    @Mock private TumblebugService tumblebugService;
    @Mock private BeylaSystemRequirementValidator beylaSystemRequirementValidator;

    @InjectMocks private BeylaFacadeService beylaFacadeService;

    private static final String NS_ID = "ns-1";
    private static final String MCI_ID = "mci-1";
    private static final String VM_ID = "vm-1";
    private static final int TEMPLATE_COUNT = 1;

    private AccessInfoDTO accessInfo;

    @BeforeEach
    void setUp() {
        accessInfo =
                AccessInfoDTO.builder()
                        .ip("192.168.1.1")
                        .port(22)
                        .user("ubuntu")
                        .sshKey("ssh-key-content")
                        .build();
    }

    @Nested
    @DisplayName("install()")
    class InstallTests {

        @Test
        @DisplayName("정상 설치: IDLE -> INSTALLING -> 스케줄러 등록")
        void normalFlow_installsSuccessfully() throws Exception {
            Task mockTask = Task.builder().id(42).status("waiting").build();
            when(semaphoreDomainService.install(
                            any(AccessInfoDTO.class),
                            eq(SemaphoreInstallMethod.INSTALL),
                            any(),
                            eq(Agent.BEYLA),
                            eq(TEMPLATE_COUNT)))
                    .thenReturn(mockTask);
            when(requestInfo.getRequestId()).thenReturn("req-123");

            beylaFacadeService.install(NS_ID, MCI_ID, VM_ID, accessInfo, TEMPLATE_COUNT);

            verify(vmService).isIdleTraceAgent(NS_ID, MCI_ID, VM_ID);
            verify(beylaSystemRequirementValidator).validateAndThrow(NS_ID, MCI_ID, VM_ID);
            verify(vmService)
                    .updateTraceAgentTaskStatus(
                            NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.INSTALLING);
            verify(vmService)
                    .updateTraceAgentTaskStatusAndTaskId(
                            NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.INSTALLING, "42");
            verify(schedulerFacadeService)
                    .scheduleTaskStatusCheck(
                            eq("req-123"),
                            eq(42),
                            eq(NS_ID),
                            eq(MCI_ID),
                            eq(VM_ID),
                            eq(SemaphoreInstallMethod.INSTALL),
                            eq(Agent.BEYLA));
        }

        @Test
        @DisplayName("이미 처리 중이면 VMAgentTaskProcessingException")
        void alreadyProcessing_throwsException() throws Exception {
            doThrow(new VMAgentTaskProcessingException("req-1", VM_ID, "traceAgent", VMAgentTaskStatus.INSTALLING))
                    .when(vmService)
                    .isIdleTraceAgent(NS_ID, MCI_ID, VM_ID);

            assertThatThrownBy(
                            () ->
                                    beylaFacadeService.install(
                                            NS_ID, MCI_ID, VM_ID, accessInfo, TEMPLATE_COUNT))
                    .isInstanceOf(VMAgentTaskProcessingException.class);

            verify(vmService, never()).updateTraceAgentTaskStatus(anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("시스템 요구사항 미충족 시 예외, 상태 IDLE 유지")
        void systemRequirementFailed_throwsException() throws Exception {
            doThrow(new BeylaSystemRequirementException("Kernel version too low"))
                    .when(beylaSystemRequirementValidator)
                    .validateAndThrow(NS_ID, MCI_ID, VM_ID);

            assertThatThrownBy(
                            () ->
                                    beylaFacadeService.install(
                                            NS_ID, MCI_ID, VM_ID, accessInfo, TEMPLATE_COUNT))
                    .isInstanceOf(BeylaSystemRequirementException.class);

            verify(vmService, never()).updateTraceAgentTaskStatus(anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Semaphore 호출 실패 시 IDLE로 롤백")
        void semaphoreFails_rollbackToIdle() throws Exception {
            when(semaphoreDomainService.install(
                            any(), any(), any(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Semaphore connection failed"));

            assertThatThrownBy(
                            () ->
                                    beylaFacadeService.install(
                                            NS_ID, MCI_ID, VM_ID, accessInfo, TEMPLATE_COUNT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Semaphore connection failed");

            verify(vmService)
                    .updateTraceAgentTaskStatus(
                            NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.INSTALLING);
            verify(vmService)
                    .updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.IDLE);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("정상 업데이트: UPDATING 상태 전이")
        void normalFlow_updatesSuccessfully() throws Exception {
            Task mockTask = Task.builder().id(55).status("waiting").build();
            when(semaphoreDomainService.install(
                            any(), eq(SemaphoreInstallMethod.UPDATE), any(), eq(Agent.BEYLA), anyInt()))
                    .thenReturn(mockTask);
            when(requestInfo.getRequestId()).thenReturn("req-456");

            beylaFacadeService.update(NS_ID, MCI_ID, VM_ID, accessInfo, TEMPLATE_COUNT);

            verify(vmService)
                    .updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.UPDATING);
            verify(vmService)
                    .updateTraceAgentTaskStatusAndTaskId(
                            NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.UPDATING, "55");
        }
    }

    @Nested
    @DisplayName("uninstall()")
    class UninstallTests {

        @Test
        @DisplayName("정상 삭제: PREPARING -> UNINSTALLING 상태 전이")
        void normalFlow_uninstallsSuccessfully() {
            Task mockTask = Task.builder().id(77).status("waiting").build();
            when(semaphoreDomainService.install(
                            any(), eq(SemaphoreInstallMethod.UNINSTALL), any(), eq(Agent.BEYLA), anyInt()))
                    .thenReturn(mockTask);
            when(requestInfo.getRequestId()).thenReturn("req-789");

            beylaFacadeService.uninstall(NS_ID, MCI_ID, VM_ID, accessInfo, TEMPLATE_COUNT);

            verify(vmService)
                    .updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.PREPARING);
            verify(vmService)
                    .updateTraceAgentTaskStatusAndTaskId(
                            NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.UNINSTALLING, "77");
        }
    }

    @Nested
    @DisplayName("restart()")
    class RestartTests {

        @Test
        @DisplayName("정상 재시작: RESTARTING -> IDLE 복귀, SUCCESS 반환")
        void normalFlow_restartsSuccessfully() {
            when(tumblebugService.restart(NS_ID, MCI_ID, VM_ID, Agent.BEYLA)).thenReturn("ok");

            List<ResultDTO> results = beylaFacadeService.restart(NS_ID, MCI_ID, VM_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo(ResponseStatus.SUCCESS);
            verify(vmService)
                    .updateTraceAgentTaskStatus(
                            NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.RESTARTING);
            verify(vmService)
                    .updateTraceAgentTaskStatus(NS_ID, MCI_ID, VM_ID, VMAgentTaskStatus.IDLE);
        }

        @Test
        @DisplayName("Tumblebug 실패 시 ERROR 반환, IDLE 복귀")
        void tumblebugFails_errorResult() {
            when(tumblebugService.restart(NS_ID, MCI_ID, VM_ID, Agent.BEYLA))
                    .thenThrow(new RuntimeException("restart failed"));

            List<ResultDTO> results = beylaFacadeService.restart(NS_ID, MCI_ID, VM_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo(ResponseStatus.ERROR);
            assertThat(results.get(0).getErrorMessage()).contains("restart failed");
        }
    }
}
