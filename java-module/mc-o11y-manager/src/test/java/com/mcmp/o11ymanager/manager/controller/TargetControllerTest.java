package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.manager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.manager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.manager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.manager.facade.TargetFacadeService;
import com.mcmp.o11ymanager.manager.model.host.TargetStatus;
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
@WebMvcTest(TargetController.class)
@MockBean(JpaMetamodelMappingContext.class)
@ActiveProfiles("test")
class TargetControllerTest {

    private static final String TAG = "[Target] Monitoring target management";

    @Autowired private MockMvc mockMvc;
    @MockBean private TargetFacadeService targetFacadeService;

    @Test
    void getTarget() throws Exception {
        TargetDTO dto =
                TargetDTO.builder()
                        .targetId("string")
                        .name("string")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("string")
                        .mciId("string")
                        .targetStatus(TargetStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();
        when(targetFacadeService.getTarget(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}",
                                "ns1",
                                "mci1",
                                "target-1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("타겟 단건 조회")
                                .summary("GetTarget")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("targetId", "타겟 ID"))
                                .responseSchema("TargetDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldObject("data", "타겟 정보"),
                                        fieldString("data.target_id", "타겟 ID"),
                                        fieldString("data.name", "타겟 이름"),
                                        fieldString("data.description", "설명").optional(),
                                        fieldNumber("data.influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum("data.target_status", "타겟 상태", TargetStatus.class)
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
                                        fieldString("data.ns_id", "NSID"),
                                        fieldString("data.mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(targetFacadeService).getTarget(any(), any(), any());
    }

    @Test
    void postTarget() throws Exception {
        TargetRequestDTO req =
                TargetRequestDTO.builder().name("string").description("string").build();
        TargetDTO dto =
                TargetDTO.builder()
                        .targetId("string")
                        .name("string")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("string")
                        .mciId("string")
                        .targetStatus(TargetStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();
        when(targetFacadeService.postTarget(any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}",
                                        "ns1",
                                        "mci1",
                                        "target-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(req)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("타겟 생성")
                                .summary("PostTarget")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("targetId", "타겟 ID"))
                                .requestSchema("TargetRequestDTO")
                                .requestFields(
                                        fieldString("name", "타겟 이름"),
                                        fieldString("description", "설명").optional())
                                .responseSchema("TargetDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldObject("data", "타겟 정보"),
                                        fieldString("data.target_id", "타겟 ID"),
                                        fieldString("data.name", "타겟 이름"),
                                        fieldString("data.description", "설명").optional(),
                                        fieldNumber("data.influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum("data.target_status", "타겟 상태", TargetStatus.class)
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
                                        fieldString("data.ns_id", "NSID"),
                                        fieldString("data.mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(targetFacadeService).postTarget(any(), any(), any(), any());
    }

    @Test
    void putTarget() throws Exception {
        TargetRequestDTO req =
                TargetRequestDTO.builder().name("string").description("string").build();
        TargetDTO dto =
                TargetDTO.builder()
                        .targetId("string")
                        .name("string")
                        .description("string")
                        .influxSeq(0L)
                        .nsId("string")
                        .mciId("string")
                        .targetStatus(TargetStatus.RUNNING)
                        .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                        .logServiceStatus(AgentServiceStatus.ACTIVE)
                        .build();
        when(targetFacadeService.putTarget(any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.put(
                                        "/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}",
                                        "ns1",
                                        "mci1",
                                        "target-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(req)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("타겟 수정")
                                .summary("PutTarget")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("targetId", "타겟 ID"))
                                .requestSchema("TargetRequestDTO")
                                .requestFields(
                                        fieldString("name", "타겟 이름"),
                                        fieldString("description", "설명").optional())
                                .responseSchema("TargetDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldObject("data", "타겟 정보"),
                                        fieldString("data.target_id", "타겟 ID"),
                                        fieldString("data.name", "타겟 이름"),
                                        fieldString("data.description", "설명").optional(),
                                        fieldNumber("data.influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum("data.target_status", "타겟 상태", TargetStatus.class)
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
                                        fieldString("data.ns_id", "NSID"),
                                        fieldString("data.mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(targetFacadeService).putTarget(any(), any(), any(), any());
    }

    @Test
    void deleteTarget() throws Exception {
        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete(
                                "/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}",
                                "ns1",
                                "mci1",
                                "target-1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("타겟 삭제")
                                .summary("DeleteTarget")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("targetId", "타겟 ID"))
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldNull("data", "null 반환"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(targetFacadeService).deleteTarget(any(), any(), any());
    }

    @Test
    void getTargetByNsMci() throws Exception {
        List<TargetDTO> list =
                List.of(
                        TargetDTO.builder()
                                .targetId("string")
                                .name("string")
                                .description("string")
                                .influxSeq(0L)
                                .nsId("string")
                                .mciId("string")
                                .targetStatus(TargetStatus.RUNNING)
                                .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                                .logServiceStatus(AgentServiceStatus.ACTIVE)
                                .build());
        when(targetFacadeService.getTargetsNsMci(any(), any())).thenReturn(list);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/monitoring/{nsId}/{mciId}/target", "ns1", "mci1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("네임스페이스/MCI별 타겟 목록 조회")
                                .summary("GetTargetByNsMci")
                                .pathParameters(
                                        paramString("nsId", "NSID"), paramString("mciId", "MCI ID"))
                                .responseSchema("TargetDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "타겟 정보 목록"),
                                        fieldString("data[].target_id", "타겟 ID"),
                                        fieldString("data[].name", "타겟 이름"),
                                        fieldString("data[].description", "설명").optional(),
                                        fieldNumber("data[].influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum(
                                                        "data[].target_status",
                                                        "타겟 상태",
                                                        TargetStatus.class)
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
                                        fieldString("data[].ns_id", "NSID"),
                                        fieldString("data[].mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(targetFacadeService).getTargetsNsMci(any(), any());
    }

    @Test
    void getAllTargets() throws Exception {
        List<TargetDTO> list =
                List.of(
                        TargetDTO.builder()
                                .targetId("string")
                                .name("string")
                                .description("string")
                                .influxSeq(0L)
                                .nsId("string")
                                .mciId("string")
                                .targetStatus(TargetStatus.RUNNING)
                                .monitoringServiceStatus(AgentServiceStatus.ACTIVE)
                                .logServiceStatus(AgentServiceStatus.ACTIVE)
                                .build());
        when(targetFacadeService.getTargets()).thenReturn(list);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/monitoring/target"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("전체 타겟 목록 조회")
                                .summary("GetAllTargets")
                                .responseSchema("TargetDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "타겟 정보 목록"),
                                        fieldString("data[].target_id", "타겟 ID"),
                                        fieldString("data[].name", "타겟 이름"),
                                        fieldString("data[].description", "설명").optional(),
                                        fieldNumber("data[].influx_seq", "인플럭스 시퀀스").optional(),
                                        fieldEnum(
                                                        "data[].target_status",
                                                        "타겟 상태",
                                                        TargetStatus.class)
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
                                        fieldString("data[].ns_id", "네임스페이이스 ID"),
                                        fieldString("data[].mci_id", "MCI ID"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(targetFacadeService).getTargets();
    }
}
