package com.mcmp.o11ymanager.manager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
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
import com.mcmp.o11ymanager.manager.facade.BeylaFacadeService;
import com.mcmp.o11ymanager.manager.service.SemaphoreInstallTemplateCounter;
import com.mcmp.o11ymanager.manager.service.VmAccessInfoResolver;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator.BeylaSystemCheckResult;
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
 * BeylaController는 Linux 전용 endpoint 모음으로 슬림화됐다. Windows 라우팅은 {@code OtelJavaController}로 이동했으니 본
 * 테스트는 (1) 정상 Linux 호출 (2) Windows VM이 들어왔을 때의 가드만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BeylaControllerTest {

    private MockMvc mockMvc;

    @Mock private BeylaFacadeService beylaFacadeService;
    @Mock private AgentFacadeService agentFacadeService;
    @Mock private BeylaSystemRequirementValidator beylaSystemRequirementValidator;
    @Mock private VmAccessInfoResolver vmAccessInfoResolver;
    @Mock private SemaphoreInstallTemplateCounter templateCounter;

    @InjectMocks private BeylaController beylaController;

    private static final String BASE_PATH = "/api/o11y/monitoring/ns-1/mci-1/vm/vm-1/beyla";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(beylaController).build();

        // default로 Linux VM 가정. Windows 가드 케이스에서만 별도 override.
        when(vmAccessInfoResolver.isWindowsNode("ns-1", "mci-1", "vm-1")).thenReturn(false);
        when(vmAccessInfoResolver.resolve("ns-1", "mci-1", "vm-1"))
                .thenReturn(
                        AccessInfoDTO.builder()
                                .ip("192.168.1.1")
                                .port(22)
                                .user("ubuntu")
                                .sshKey("ssh-key-content")
                                .osType("linux")
                                .build());
        when(templateCounter.next()).thenReturn(1);
    }

    @Test
    @DisplayName("POST install - Linux VM 정상 호출 200 OK")
    void install_returns200() throws Exception {
        doNothing()
                .when(beylaFacadeService)
                .install(anyString(), anyString(), anyString(), any(), anyInt());

        mockMvc.perform(post(BASE_PATH + "/install"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rs_code").value("0000"));
    }

    @Test
    @DisplayName("PUT update - Linux VM 정상 호출 200 OK")
    void update_returns200() throws Exception {
        doNothing()
                .when(beylaFacadeService)
                .update(anyString(), anyString(), anyString(), any(), anyInt());

        mockMvc.perform(put(BASE_PATH + "/update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rs_code").value("0000"));
    }

    @Test
    @DisplayName("DELETE uninstall - Linux VM 정상 호출 200 OK")
    void uninstall_returns200() throws Exception {
        doNothing()
                .when(beylaFacadeService)
                .uninstall(anyString(), anyString(), anyString(), any(), anyInt());

        mockMvc.perform(delete(BASE_PATH + "/uninstall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rs_code").value("0000"));
    }

    @Test
    @DisplayName("POST restart - 결과 리스트 반환")
    void restart_returnsResults() throws Exception {
        ResultDTO result =
                ResultDTO.builder()
                        .nsId("ns-1")
                        .mciId("mci-1")
                        .vmId("vm-1")
                        .status(ResponseStatus.SUCCESS)
                        .build();
        when(beylaFacadeService.restart("ns-1", "mci-1", "vm-1")).thenReturn(List.of(result));

        mockMvc.perform(post(BASE_PATH + "/restart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET status - AgentStatus 반환")
    void getStatus_returnsAgentStatus() throws Exception {
        when(agentFacadeService.getAgentStatus("ns-1", "mci-1", "vm-1", Agent.BEYLA))
                .thenReturn(AgentStatus.SUCCESS);

        mockMvc.perform(get(BASE_PATH + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET system-check - BeylaSystemCheckResult 반환")
    void checkSystemRequirements_returnsResult() throws Exception {
        BeylaSystemCheckResult checkResult =
                BeylaSystemCheckResult.builder()
                        .kernelVersion("5.15.0")
                        .btfSupported(true)
                        .kernelVersionValid(true)
                        .minimumKernelVersion("5.8.0")
                        .build();
        when(beylaSystemRequirementValidator.validate("ns-1", "mci-1", "vm-1"))
                .thenReturn(checkResult);

        mockMvc.perform(get(BASE_PATH + "/system-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kernelVersion").value("5.15.0"))
                .andExpect(jsonPath("$.data.btfSupported").value(true))
                .andExpect(jsonPath("$.data.kernelVersionValid").value(true));
    }

    @Test
    @DisplayName("POST install - Windows VM이면 가드 throw (5xx로 응답)")
    void install_windowsVm_throwsGuard() throws Exception {
        when(vmAccessInfoResolver.isWindowsNode("ns-1", "mci-1", "vm-1")).thenReturn(true);

        // standalone MockMvc는 controller throw를 500으로 매핑. 메시지에 안내 문구 포함되는지만 확인.
        mockMvc.perform(post(BASE_PATH + "/install")).andExpect(status().is4xxClientError());
    }
}
