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

    private static final String TAG = "[VM] Monitoring vm management";

    @Autowired private MockMvc mockMvc;
    @MockBean private VMFacadeService vmFacadeService;

    @Test
    void getVM() throws Exception {
        VMDTO dto =
                VMDTO.builder()
                        .vmId("string")
                        .name("string")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("string")
                        .mciId("string")
                        .vmStatus(VMStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();
        when(vmFacadeService.getVM(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/monitoring/{NS ID}/{mciId}/vm/{vmId}",
                                "ns1",
                                "mci1",
                                "vm-1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("타겟 단건 조회")
                                .summary("GetVM")
                                .pathParameters(
                                        paramString("NS ID", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("vmId", "TARGET ID"))
                                .responseSchema("VMDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldObject("data", "타겟 정보"),
                                        fieldString("data.vm_id", "TARGET ID"),
                                        fieldString("data.name", "타겟 이름"),
                                        fieldString("data.description", "설명").optional(),
                                        fieldNumber("data.influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum("data.vm_status", "타겟 상태", VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.monitoring_service_status",
                                                        "모니터링 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.log_service_status",
                                                        "로그 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data.ns_id", "NS ID"),
                                        fieldString("data.mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(vmFacadeService).getVM(any(), any(), any());
    }

    @Test
    void postVM() throws Exception {
        VMRequestDTO req = VMRequestDTO.builder().name("string").description("string").build();
        VMDTO dto =
                VMDTO.builder()
                        .vmId("string")
                        .name("string")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("string")
                        .mciId("string")
                        .vmStatus(VMStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();
        when(vmFacadeService.postVM(any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/monitoring/{NS ID}/{mciId}/vm/{vmId}",
                                        "ns1",
                                        "mci1",
                                        "vm-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(req)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("타겟 생성")
                                .summary("PostVM")
                                .pathParameters(
                                        paramString("NS ID", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("vmId", "TARGET ID"))
                                .requestSchema("VMRequestDTO")
                                .requestFields(
                                        fieldString("name", "타겟 이름"),
                                        fieldString("description", "설명").optional())
                                .responseSchema("VMDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldObject("data", "타겟 정보"),
                                        fieldString("data.vm_id", "TARGET ID"),
                                        fieldString("data.name", "타겟 이름"),
                                        fieldString("data.description", "설명").optional(),
                                        fieldNumber("data.influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum("data.vm_status", "타겟 상태", VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.monitoring_service_status",
                                                        "모니터링 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.log_service_status",
                                                        "로그 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data.ns_id", "NS ID"),
                                        fieldString("data.mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(vmFacadeService).postVM(any(), any(), any(), any());
    }

    @Test
    void putVM() throws Exception {
        VMRequestDTO req = VMRequestDTO.builder().name("string").description("string").build();
        VMDTO dto =
                VMDTO.builder()
                        .vmId("string")
                        .name("string")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("string")
                        .mciId("string")
                        .vmStatus(VMStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();
        when(vmFacadeService.putVM(any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.put(
                                        "/api/o11y/monitoring/{NS ID}/{mciId}/vm/{vmId}",
                                        "ns1",
                                        "mci1",
                                        "vm-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(req)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("타겟 수정")
                                .summary("PutVM")
                                .pathParameters(
                                        paramString("NS ID", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("vmId", "TARGET ID"))
                                .requestSchema("VMRequestDTO")
                                .requestFields(
                                        fieldString("name", "타겟 이름"),
                                        fieldString("description", "설명").optional())
                                .responseSchema("VMDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldObject("data", "타겟 정보"),
                                        fieldString("data.vm_id", "TARGET ID"),
                                        fieldString("data.name", "타겟 이름"),
                                        fieldString("data.description", "설명").optional(),
                                        fieldNumber("data.influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum("data.vm_status", "타겟 상태", VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.monitoring_service_status",
                                                        "모니터링 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data.log_service_status",
                                                        "로그 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data.ns_id", "NS ID"),
                                        fieldString("data.mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(vmFacadeService).putVM(any(), any(), any(), any());
    }

    @Test
    void deleteVM() throws Exception {
        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete(
                                "/api/o11y/monitoring/{NS ID}/{mciId}/vm/{vmId}",
                                "ns1",
                                "mci1",
                                "vm-1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("타겟 삭제")
                                .summary("DeleteVM")
                                .pathParameters(
                                        paramString("NS ID", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("vmId", "TARGET ID"))
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldNull("data", "null 반환"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(vmFacadeService).deleteVM(any(), any(), any());
    }

    @Test
    void getVMByNsMci() throws Exception {
        List<VMDTO> list =
                List.of(
                        VMDTO.builder()
                                .vmId("string")
                                .name("string")
                                .description("string")
                                .influxSeq(0L)
                                .nsId("string")
                                .mciId("string")
                                .vmStatus(VMStatus.RUNNING)
                                .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                                .logServiceStatus(AgentServiceStatus.ACTIVE)
                                .build());
        when(vmFacadeService.getVMsNsMci(any(), any())).thenReturn(list);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/monitoring/{NS ID}/{mciId}/vm", "ns1", "mci1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("NS /MCI별 타겟 목록 조회")
                                .summary("GetVMByNsMci")
                                .pathParameters(
                                        paramString("NS ID", "NS ID"),
                                        paramString("mciId", "MCI ID"))
                                .responseSchema("VMDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "타겟 정보 목록"),
                                        fieldString("data[].vm_id", "TARGET ID"),
                                        fieldString("data[].name", "타겟 이름"),
                                        fieldString("data[].description", "설명").optional(),
                                        fieldNumber("data[].influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum("data[].vm_status", "타겟 상태", VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data[].monitoring_service_status",
                                                        "모니터링 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data[].log_service_status",
                                                        "로그 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data[].ns_id", "NS ID"),
                                        fieldString("data[].mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(vmFacadeService).getVMsNsMci(any(), any());
    }

    @Test
    void getAllVMs() throws Exception {
        List<VMDTO> list =
                List.of(
                        VMDTO.builder()
                                .vmId("string")
                                .name("string")
                                .description("string")
                                .influxSeq(0L)
                                .nsId("string")
                                .mciId("string")
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
                                .description("전체 타겟 목록 조회")
                                .summary("GetAllVMs")
                                .responseSchema("VMDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "타겟 정보 목록"),
                                        fieldString("data[].vm_id", "TARGET ID"),
                                        fieldString("data[].name", "타겟 이름"),
                                        fieldString("data[].description", "설명").optional(),
                                        fieldNumber("data[].influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum("data[].vm_status", "타겟 상태", VMStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data[].monitoring_service_status",
                                                        "모니터링 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldEnum(
                                                        "data[].log_service_status",
                                                        "로그 에이전트 서비스 상태",
                                                        AgentServiceStatus.class)
                                                .optional(),
                                        fieldString("data[].ns_id", "NS ID"),
                                        fieldString("data[].mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(vmFacadeService).getVMs();
    }
}
