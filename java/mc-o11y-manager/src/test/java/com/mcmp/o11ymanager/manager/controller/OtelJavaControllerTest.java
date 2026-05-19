package com.mcmp.o11ymanager.manager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.facade.AgentFacadeService;
import com.mcmp.o11ymanager.manager.facade.OtelJavaFacadeService;
import com.mcmp.o11ymanager.manager.service.SemaphoreInstallTemplateCounter;
import com.mcmp.o11ymanager.manager.service.VmAccessInfoResolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Windows VM 전용 trace agent controller 테스트. URL prefix는 {@code /windows-trace-agent}. Linux VM이
 * 들어왔을 때는 가드 throw, Windows일 때는 OtelJavaFacadeService로 위임이 잘 되는지 검증.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtelJavaControllerTest {

    private MockMvc mockMvc;

    @Mock private OtelJavaFacadeService otelJavaFacadeService;
    @Mock private AgentFacadeService agentFacadeService;
    @Mock private VmAccessInfoResolver vmAccessInfoResolver;
    @Mock private SemaphoreInstallTemplateCounter templateCounter;

    @InjectMocks private OtelJavaController otelJavaController;

    private static final String BASE_PATH =
            "/api/o11y/monitoring/testns01/win2019-os01/vm/win-1/windows-trace-agent";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(otelJavaController).build();

        // default로 Windows VM 가정. Linux 가드 케이스에서만 별도 override.
        when(vmAccessInfoResolver.isWindowsNode("testns01", "win2019-os01", "win-1")).thenReturn(true);
        when(vmAccessInfoResolver.resolve("testns01", "win2019-os01", "win-1"))
                .thenReturn(
                        AccessInfoDTO.builder()
                                .ip("192.168.110.113")
                                .port(5985)
                                .user("cb-user")
                                .password("Win!Password123")
                                .osType("windows")
                                .winrmScheme("http")
                                .build());
        when(templateCounter.next()).thenReturn(1);
    }

    @Test
    @DisplayName("POST install - Windows VM 정상 호출 200 OK, OtelJavaFacadeService로 위임")
    void install_dispatchesToOtelJava() throws Exception {
        doNothing()
                .when(otelJavaFacadeService)
                .install(anyString(), anyString(), anyString(), any(), anyInt());

        mockMvc.perform(post(BASE_PATH + "/install"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rs_code").value("0000"));

        verify(otelJavaFacadeService)
                .install(anyString(), anyString(), anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("PUT update - 정상 호출 200 OK")
    void update_returns200() throws Exception {
        doNothing()
                .when(otelJavaFacadeService)
                .update(anyString(), anyString(), anyString(), any(), anyInt());

        mockMvc.perform(put(BASE_PATH + "/update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rs_code").value("0000"));
    }

    @Test
    @DisplayName("DELETE uninstall - 정상 호출 200 OK")
    void uninstall_returns200() throws Exception {
        doNothing()
                .when(otelJavaFacadeService)
                .uninstall(anyString(), anyString(), anyString(), any(), anyInt());

        mockMvc.perform(delete(BASE_PATH + "/uninstall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rs_code").value("0000"));
    }

    @Test
    @DisplayName("POST restart - 미지원 ERROR 결과 반환")
    void restart_returnsErrorResult() throws Exception {
        ResultDTO err =
                ResultDTO.builder()
                        .nsId("testns01")
                        .mciId("win2019-os01")
                        .vmId("win-1")
                        .status(ResponseStatus.ERROR)
                        .errorMessage("Restart is not supported")
                        .build();
        when(otelJavaFacadeService.restart("testns01", "win2019-os01", "win-1"))
                .thenReturn(List.of(err));

        mockMvc.perform(post(BASE_PATH + "/restart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ERROR"));
    }

    @Test
    @DisplayName("GET status - Agent.OTEL_JAVA_AGENT 기준으로 조회")
    void getStatus_usesOtelJavaAgentEnum() throws Exception {
        when(agentFacadeService.getAgentStatus(
                        "testns01", "win2019-os01", "win-1", Agent.OTEL_JAVA_AGENT))
                .thenReturn(AgentStatus.SUCCESS);

        mockMvc.perform(get(BASE_PATH + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST install - Linux VM이면 가드 throw (5xx)")
    void install_linuxVm_throwsGuard() throws Exception {
        when(vmAccessInfoResolver.isWindowsNode("testns01", "win2019-os01", "win-1")).thenReturn(false);

        mockMvc.perform(post(BASE_PATH + "/install"))
                .andExpect(status().is4xxClientError());
    }
}
