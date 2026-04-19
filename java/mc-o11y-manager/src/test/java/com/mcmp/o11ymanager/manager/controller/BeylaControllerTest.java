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

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.facade.AgentFacadeService;
import com.mcmp.o11ymanager.manager.facade.BeylaFacadeService;
import com.mcmp.o11ymanager.manager.port.TumblebugPort;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BeylaControllerTest {

    private MockMvc mockMvc;

    @Mock private BeylaFacadeService beylaFacadeService;
    @Mock private AgentFacadeService agentFacadeService;
    @Mock private TumblebugPort tumblebugPort;
    @Mock private BeylaSystemRequirementValidator beylaSystemRequirementValidator;

    @InjectMocks private BeylaController beylaController;

    private static final String BASE_PATH = "/api/o11y/monitoring/ns-1/mci-1/vm/vm-1/beyla";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(beylaController).build();

        TumblebugMCI.Vm mockVm = new TumblebugMCI.Vm();
        mockVm.setPublicIP("192.168.1.1");
        mockVm.setSshPort("22");
        mockVm.setVmUserName("ubuntu");
        mockVm.setSshKeyId("key-1");
        when(tumblebugPort.getVM("ns-1", "mci-1", "vm-1")).thenReturn(mockVm);

        TumblebugSshKey mockSshKey = new TumblebugSshKey();
        mockSshKey.setPrivateKey("ssh-key-content");
        when(tumblebugPort.getSshKey("ns-1", "key-1")).thenReturn(mockSshKey);
    }

    @Test
    @DisplayName("POST install - 200 OK")
    void install_returns200() throws Exception {
        doNothing()
                .when(beylaFacadeService)
                .install(anyString(), anyString(), anyString(), any(), anyInt());

        mockMvc.perform(post(BASE_PATH + "/install"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rs_code").value("0000"));
    }

    @Test
    @DisplayName("PUT update - 200 OK")
    void update_returns200() throws Exception {
        doNothing()
                .when(beylaFacadeService)
                .update(anyString(), anyString(), anyString(), any(), anyInt());

        mockMvc.perform(put(BASE_PATH + "/update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rs_code").value("0000"));
    }

    @Test
    @DisplayName("DELETE uninstall - 200 OK")
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
}
