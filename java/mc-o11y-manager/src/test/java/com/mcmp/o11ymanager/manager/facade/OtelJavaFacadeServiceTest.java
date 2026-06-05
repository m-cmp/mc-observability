package com.mcmp.o11ymanager.manager.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.manager.exception.vm.VMAgentTaskProcessingException;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.semaphore.Task;
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
class OtelJavaFacadeServiceTest {

    @Mock private VMService vmService;
    @Mock private RequestInfo requestInfo;
    @Mock private SemaphoreDomainService semaphoreDomainService;
    @Mock private SchedulerFacadeService schedulerFacadeService;
    @Mock private TumblebugService tumblebugService;
    @Mock private OtelJavaConfigFacadeService otelJavaConfigFacadeService;

    @InjectMocks private OtelJavaFacadeService otelJavaFacadeService;

    private static final String NS_ID = "ns-1";
    private static final String INFRA_ID = "mci-1";
    private static final String NODE_ID = "vm-1";
    private static final int TEMPLATE_COUNT = 1;

    private AccessInfoDTO accessInfo;

    @BeforeEach
    void setUp() {
        accessInfo =
                AccessInfoDTO.builder()
                        .ip("192.168.1.10")
                        .port(22)
                        .user("Administrator")
                        .sshKey("ssh-key-content")
                        .build();
    }

    @Nested
    @DisplayName("install()")
    class InstallTests {

        @Test
        @DisplayName(
                "정상 설치: IDLE -> INSTALLING -> 스케줄러 등록 + configContent 전달 (Agent.OTEL_JAVA_AGENT)")
        void normalFlow_installsSuccessfully() throws Exception {
            Task mockTask = Task.builder().id(101).status("waiting").build();
            String generatedConfig = "JAVA_TOOL_OPTIONS=-javaagent:C:\\opentelemetry\\jar\n";
            when(otelJavaConfigFacadeService.initOtelJavaConfig(NS_ID, INFRA_ID, NODE_ID))
                    .thenReturn(generatedConfig);
            when(semaphoreDomainService.install(
                            any(AccessInfoDTO.class),
                            eq(SemaphoreInstallMethod.INSTALL),
                            eq(generatedConfig),
                            eq(Agent.OTEL_JAVA_AGENT),
                            eq(TEMPLATE_COUNT)))
                    .thenReturn(mockTask);
            when(requestInfo.getRequestId()).thenReturn("req-otel-1");

            otelJavaFacadeService.install(NS_ID, INFRA_ID, NODE_ID, accessInfo, TEMPLATE_COUNT);

            verify(vmService).isIdleTraceAgent(NS_ID, INFRA_ID, NODE_ID);
            verify(otelJavaConfigFacadeService).initOtelJavaConfig(NS_ID, INFRA_ID, NODE_ID);
            verify(vmService)
                    .updateTraceAgentTaskStatus(
                            NS_ID, INFRA_ID, NODE_ID, VMAgentTaskStatus.INSTALLING);
            verify(vmService)
                    .updateTraceAgentTaskStatusAndTaskId(
                            NS_ID, INFRA_ID, NODE_ID, VMAgentTaskStatus.INSTALLING, "101");
            verify(schedulerFacadeService)
                    .scheduleTaskStatusCheck(
                            eq("req-otel-1"),
                            eq(101),
                            eq(NS_ID),
                            eq(INFRA_ID),
                            eq(NODE_ID),
                            eq(SemaphoreInstallMethod.INSTALL),
                            eq(Agent.OTEL_JAVA_AGENT));
        }

        @Test
        @DisplayName("이미 처리 중이면 VMAgentTaskProcessingException")
        void alreadyProcessing_throwsException() throws Exception {
            doThrow(
                            new VMAgentTaskProcessingException(
                                    "req-1", NODE_ID, "traceAgent", VMAgentTaskStatus.INSTALLING))
                    .when(vmService)
                    .isIdleTraceAgent(NS_ID, INFRA_ID, NODE_ID);

            assertThatThrownBy(
                            () ->
                                    otelJavaFacadeService.install(
                                            NS_ID, INFRA_ID, NODE_ID, accessInfo, TEMPLATE_COUNT))
                    .isInstanceOf(VMAgentTaskProcessingException.class);

            verify(vmService, never())
                    .updateTraceAgentTaskStatus(anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Semaphore 호출 실패 시 IDLE로 롤백")
        void semaphoreFails_rollbackToIdle() throws Exception {
            when(otelJavaConfigFacadeService.initOtelJavaConfig(NS_ID, INFRA_ID, NODE_ID))
                    .thenReturn("dummy");
            when(semaphoreDomainService.install(any(), any(), any(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Semaphore connection failed"));

            assertThatThrownBy(
                            () ->
                                    otelJavaFacadeService.install(
                                            NS_ID, INFRA_ID, NODE_ID, accessInfo, TEMPLATE_COUNT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Semaphore connection failed");

            verify(vmService)
                    .updateTraceAgentTaskStatus(
                            NS_ID, INFRA_ID, NODE_ID, VMAgentTaskStatus.INSTALLING);
            verify(vmService)
                    .updateTraceAgentTaskStatus(NS_ID, INFRA_ID, NODE_ID, VMAgentTaskStatus.IDLE);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("정상 업데이트: UPDATING 상태 전이 + Agent.OTEL_JAVA_AGENT 전달")
        void normalFlow_updatesSuccessfully() throws Exception {
            Task mockTask = Task.builder().id(202).status("waiting").build();
            when(semaphoreDomainService.install(
                            any(),
                            eq(SemaphoreInstallMethod.UPDATE),
                            any(),
                            eq(Agent.OTEL_JAVA_AGENT),
                            anyInt()))
                    .thenReturn(mockTask);
            when(requestInfo.getRequestId()).thenReturn("req-otel-2");

            otelJavaFacadeService.update(NS_ID, INFRA_ID, NODE_ID, accessInfo, TEMPLATE_COUNT);

            verify(vmService)
                    .updateTraceAgentTaskStatus(
                            NS_ID, INFRA_ID, NODE_ID, VMAgentTaskStatus.UPDATING);
            verify(vmService)
                    .updateTraceAgentTaskStatusAndTaskId(
                            NS_ID, INFRA_ID, NODE_ID, VMAgentTaskStatus.UPDATING, "202");
        }
    }

    @Nested
    @DisplayName("uninstall()")
    class UninstallTests {

        @Test
        @DisplayName("정상 삭제: PREPARING -> UNINSTALLING 상태 전이")
        void normalFlow_uninstallsSuccessfully() {
            Task mockTask = Task.builder().id(303).status("waiting").build();
            when(semaphoreDomainService.install(
                            any(),
                            eq(SemaphoreInstallMethod.UNINSTALL),
                            any(),
                            eq(Agent.OTEL_JAVA_AGENT),
                            anyInt()))
                    .thenReturn(mockTask);
            when(requestInfo.getRequestId()).thenReturn("req-otel-3");

            otelJavaFacadeService.uninstall(NS_ID, INFRA_ID, NODE_ID, accessInfo, TEMPLATE_COUNT);

            verify(vmService)
                    .updateTraceAgentTaskStatus(
                            NS_ID, INFRA_ID, NODE_ID, VMAgentTaskStatus.PREPARING);
            verify(vmService)
                    .updateTraceAgentTaskStatusAndTaskId(
                            NS_ID, INFRA_ID, NODE_ID, VMAgentTaskStatus.UNINSTALLING, "303");
        }
    }

    @Nested
    @DisplayName("restart()")
    class RestartTests {

        @Test
        @DisplayName("Tumblebug.restart 호출 시 Agent.OTEL_JAVA_AGENT 전달, ERROR 반환")
        void restart_callsWithOtelJavaAgent() {
            // OTel Java agent는 TumblebugServiceImpl에서 restart-unsupported로 throw됨.
            // facade 입장에선 catch하고 ERROR 결과 반환.
            when(tumblebugService.restart(NS_ID, INFRA_ID, NODE_ID, Agent.OTEL_JAVA_AGENT))
                    .thenThrow(new RuntimeException("Restart is not supported"));

            List<ResultDTO> results = otelJavaFacadeService.restart(NS_ID, INFRA_ID, NODE_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo(ResponseStatus.ERROR);
            assertThat(results.get(0).getErrorMessage()).contains("Restart is not supported");
        }
    }
}
