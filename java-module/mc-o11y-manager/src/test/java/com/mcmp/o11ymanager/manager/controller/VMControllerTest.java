package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.manager.facade.VMFacadeService;
import com.mcmp.o11ymanager.manager.model.host.VMStatus;
import com.mcmp.o11ymanager.util.ApiDocumentation;
import com.mcmp.o11ymanager.util.JsonConverter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc
@WebMvcTest(VMController.class)
@MockBean(JpaMetamodelMappingContext.class)
@ActiveProfiles("test")
class VMControllerTest {

    private static final String TAG = "[Manager] Monitoring VM Management";

    @Autowired private MockMvc mockMvc;
    @MockBean private VMFacadeService vmFacadeService;

    @Test
    void getVM() throws Exception {
        VMDTO dto =
                VMDTO.builder()
                        .vmId("vm-1")
                        .name("mcmp-vm")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("ns-1")
                        .mciId("mci-1")
                        .vmStatus(VMStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();

        when(vmFacadeService.getVM(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}",
                                "ns1",
                                "mci1",
                                "vm-1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get target (VM)")
                                .summary("GetVM")
                                .pathParameters(
                                        paramString("nsId", "Namespace ID (e.g., ns-1)"),
                                        paramString("mciId", "MCI ID (e.g., mci-1)"),
                                        paramString("vmId", "VM ID (e.g., vm-1)"))
                                .responseSchema("ResBody")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldObject("data", "Target information"),
                                        fieldString("data.name", "Target name (e.g., mcmp-vm)"),
                                        fieldString("data.description", "Description").optional(),
                                        fieldString("data.vm_id", "VM ID (e.g., vm-1)"),
                                        fieldNumber("data.influx_seq", "Influx sequence")
                                                .optional(),
                                        fieldEnum("data.vm_status", "Target status", VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.monitoring_service_status",
                                                        "Monitoring agent service status",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.log_service_status",
                                                        "Log agent service status",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data.ns_id", "Namespace ID (e.g., ns-1)"),
                                        fieldString("data.mci_id", "MCI ID (e.g., mci-1)"),
                                        fieldString("error_message", "Error message").optional())
                                .build());

        verify(vmFacadeService).getVM(any(), any(), any());
    }

    @Test
    void postVM() throws Exception {
        VMRequestDTO req = VMRequestDTO.builder().name("mcmp-vm").description("string").build();

        VMDTO dto =
                VMDTO.builder()
                        .vmId("vm-1")
                        .name("mcmp-vm")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("ns-1")
                        .mciId("mci-1")
                        .vmStatus(VMStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();

        when(vmFacadeService.postVM(any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}",
                                        "ns1",
                                        "mci1",
                                        "vm-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(req)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Create target (VM)")
                                .summary("PostVM")
                                .pathParameters(
                                        paramString("nsId", "Namespace ID (e.g., ns-1)"),
                                        paramString("mciId", "MCI ID (e.g., mci-1)"),
                                        paramString("vmId", "VM ID (e.g., vm-1)"))
                                .requestSchema("VMRequestDTO")
                                .requestFields(
                                        fieldString("name", "Target name (e.g., mcmp-vm)"),
                                        fieldString("description", "Description").optional())
                                .responseSchema("ResBody")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldObject("data", "Target information (nullable)")
                                                .optional(),
                                        fieldString("error_message", "Error message").optional())
                                .build());

        verify(vmFacadeService).postVM(any(), any(), any(), any());
    }

    @Test
    void putVM() throws Exception {
        VMRequestDTO req = VMRequestDTO.builder().name("mcmp-vm").description("string").build();
        VMDTO dto =
                VMDTO.builder()
                        .vmId("vm-1")
                        .name("mcmp-vm")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("ns-1")
                        .mciId("mci-1")
                        .vmStatus(VMStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();
        when(vmFacadeService.putVM(any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.put(
                                        "/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}",
                                        "ns1",
                                        "mci1",
                                        "vm-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(req)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Update target")
                                .summary("PutVM")
                                .pathParameters(
                                        paramString("nsId", "nsId (e.g., ns-1)"),
                                        paramString("mciId", "mciId (e.g., mci-1)"),
                                        paramString("vmId", "vmId (e.g., vm-1)"))
                                .requestSchema("VMRequestDTO")
                                .requestFields(
                                        fieldString("name", "Target name (e.g., mcmp-vm)"),
                                        fieldString("description", "Description").optional())
                                .responseSchema("VMDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g.,  0000)"),
                                        fieldString("rs_msg", "Response message (e.g.,  Success)"),
                                        fieldObject("data", "Target information"),
                                        fieldString("data.vm_id", "vmId (e.g.,  vm-1)"),
                                        fieldString("data.name", "Target name (e.g.,  mcmp-vm)"),
                                        fieldString("data.description", "Description").optional(),
                                        fieldNumber("data.influx_seq", "Influx sequence")
                                                .optional(),
                                        fieldEnum("data.vm_status", "Target status", VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.monitoring_service_status",
                                                        "Monitoring agent service status",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.log_service_status",
                                                        "Log agent service status",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data.ns_id", "nsId (e.g., ns-1)"),
                                        fieldString("data.mci_id", "mciId (e.g., mci-1)"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(vmFacadeService).putVM(any(), any(), any(), any());
    }

    @Test
    void deleteVM() throws Exception {
        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete(
                                "/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}",
                                "ns1",
                                "mci1",
                                "vm-1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Delete target")
                                .summary("DeleteVM")
                                .pathParameters(
                                        paramString("nsId", "nsId (e.g., ns-1)"),
                                        paramString("mciId", "mciId (e.g., mci-1)"),
                                        paramString("vmId", "vmId (e.g., vm-1)"))
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldNull("data", "null return"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(vmFacadeService).deleteVM(any(), any(), any());
    }

    @Test
    void getVMByNsMci() throws Exception {
        List<VMDTO> list =
                List.of(
                        VMDTO.builder()
                                .vmId("vm-1")
                                .name("mcmp-vm")
                                .description("string")
                                .influxSeq(0L)
                                .nsId("ns-1")
                                .mciId("mci-1")
                                .vmStatus(VMStatus.RUNNING)
                                .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                                .logServiceStatus(AgentServiceStatus.ACTIVE)
                                .build());
        when(vmFacadeService.getVMsNsMci(any(), any())).thenReturn(list);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/monitoring/{nsId}/{mciId}/vm", "ns1", "mci1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Retrieve target list by NS/MCI")
                                .summary("GetVMByNsMci")
                                .pathParameters(
                                        paramString("nsId", "nsId (e.g., ns-1)"),
                                        paramString("mciId", "mciId (e.g., mci-1)"))
                                .responseSchema("VMDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldArray("data", "Target information list"),
                                        fieldString("data[].vm_id", "vmId"),
                                        fieldString("data[].name", "Target name(e.g., mcmp-vm)"),
                                        fieldString("data[].description", "Description").optional(),
                                        fieldNumber("data[].influx_seq", "Influx sequence")
                                                .optional(),
                                        fieldEnum(
                                                        "data[].vm_status",
                                                        "Target status",
                                                        VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data[].monitoring_service_status",
                                                        "Monitoring agent service status",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data[].log_service_status",
                                                        "Log agent service status",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data[].ns_id", "nsId (e.g., ns-1)"),
                                        fieldString("data[].mci_id", "mciId (e.g., mci-1)"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(vmFacadeService).getVMsNsMci(any(), any());
    }

    @Test
    void getAllVMs() throws Exception {
        List<VMDTO> list =
                List.of(
                        VMDTO.builder()
                                .vmId("vm-1")
                                .name("mcmp-vm")
                                .description("string")
                                .influxSeq(0L)
                                .nsId("ns-1")
                                .mciId("mci-1")
                                .vmStatus(VMStatus.RUNNING)
                                .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                                .logServiceStatus(AgentServiceStatus.ACTIVE)
                                .build());
        when(vmFacadeService.getVMs()).thenReturn(list);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/monitoring/vm"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Retrieve all targets")
                                .summary("GetAllVMs")
                                .responseSchema("VMDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldArray("data", "Target information list"),
                                        fieldString("data[].vm_id", "vmId"),
                                        fieldString("data[].name", "Target name (e.g., mcmp-vm)"),
                                        fieldString("data[].description", "Description").optional(),
                                        fieldNumber("data[].influx_seq", "Influx sequence")
                                                .optional(),
                                        fieldEnum(
                                                        "data[].vm_status",
                                                        "Target status",
                                                        VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data[].monitoring_service_status",
                                                        "Monitoring agent service status",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data[].log_service_status",
                                                        "Log agent service status",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data[].ns_id", "nsId (e.g., ns-1)"),
                                        fieldString("data[].mci_id", "mciId (e.g.,  ci-1)"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(vmFacadeService).getVMs();
    }
}
