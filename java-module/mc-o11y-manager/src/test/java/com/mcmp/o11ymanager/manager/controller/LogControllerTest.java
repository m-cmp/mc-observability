package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.manager.dto.log.LabelResultDto;
import com.mcmp.o11ymanager.manager.dto.log.LogSummaryDto;
import com.mcmp.o11ymanager.manager.dto.log.LogVolumeResponseDto;
import com.mcmp.o11ymanager.manager.facade.LogFacadeService;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.util.ApiDocumentation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc
@WebMvcTest(LogController.class)
@MockBean(JpaMetamodelMappingContext.class)
@MockBean(RequestInfo.class)
@ActiveProfiles("test")
class LogControllerTest {

    private static final String TAG = "[Monitoring log] Monitoring log";

    @Autowired private MockMvc mockMvc;

    @MockBean private LogFacadeService logFacadeService;

    @MockBean private RequestInfo requestInfo;

    @Test
    void queryRangeLogs() throws Exception {
        LogSummaryDto.ResultDto mockResult =
                LogSummaryDto.ResultDto.builder()
                        .status("string")
                        .data(
                                List.of(
                                        LogSummaryDto.LogEntryDto.builder()
                                                .labels(Map.of("app", "string", "env", "string"))
                                                .timestamp(0D)
                                                .value("string")
                                                .build()))
                        .stats(
                                LogSummaryDto.StatsDto.builder()
                                        .totalBytesProcessed(0L)
                                        .totalLinesProcessed(0L)
                                        .execTime(0.0)
                                        .totalEntriesReturned(0)
                                        .build())
                        .build();

        // ✅ Stub 확실히 걸기
        when(logFacadeService.getRangeLogs(
                        anyString(),
                        anyString(),
                        anyString(),
                        ArgumentMatchers.anyInt(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()))
                .thenReturn(mockResult);

        when(requestInfo.getRequestId()).thenReturn("string");

        mockMvc.perform(
                        get("/api/v1/log/query_range")
                                .param("query", "test-query")
                                .param("start", "2025-09-01T00:00:00Z")
                                .param("end", "2025-09-02T00:00:00Z")
                                .param("limit", "10")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("로그 기간 조회 API")
                                .description("특정 쿼리에 대한 기간별 로그 데이터를 조회한다.")
                                .responseFields(
                                        fieldString("timestamp", "응답 시간"),
                                        fieldString("status", "응답 상태"),
                                        fieldString("code", "응답 코드"),
                                        fieldString("message", "응답 메시지"),
                                        fieldString("requestId", "요청 ID"),
                                        fieldObject("data.result", "로그 결과 객체").optional(),
                                        fieldString("data.result.status", "로그 상태").optional(),
                                        fieldObject("data.result.data[].labels", "로그 라벨")
                                                .optional(),
                                        fieldString(
                                                        "data.result.data[].labels.*",
                                                        "라벨 key-value (동적)")
                                                .optional(),
                                        fieldNumber("data.result.data[].timestamp", "타임스탬프")
                                                .optional(),
                                        fieldString("data.result.data[].value", "로그 값").optional(),
                                        fieldNumber(
                                                        "data.result.stats.totalBytesProcessed",
                                                        "처리된 바이트 수")
                                                .optional(),
                                        fieldNumber(
                                                        "data.result.stats.totalLinesProcessed",
                                                        "처리된 라인 수")
                                                .optional(),
                                        fieldNumber("data.result.stats.execTime", "실행 시간(ms)")
                                                .optional(),
                                        fieldNumber(
                                                        "data.result.stats.totalEntriesReturned",
                                                        "반환된 엔트리 수")
                                                .optional())
                                .build());
    }

    @Test
    void getLogVolumes() throws Exception {
        LogVolumeResponseDto mockResult =
                LogVolumeResponseDto.builder()
                        .data(
                                List.of(
                                        LogVolumeResponseDto.MetricResultDto.builder()
                                                .metric(Map.of("app", "string", "env", "string"))
                                                .values(
                                                        List.of(
                                                                LogVolumeResponseDto
                                                                        .TimeSeriesValueDto
                                                                        .builder()
                                                                        .timestamp(0L)
                                                                        .value("string")
                                                                        .build()))
                                                .build()))
                        .build();

        when(logFacadeService.getLogVolumes(anyString(), anyString(), anyString(), any()))
                .thenReturn(mockResult);
        when(requestInfo.getRequestId()).thenReturn("string");

        mockMvc.perform(
                        get("/api/v1/log/log_volumes")
                                .param("query", "test-query")
                                .param("start", "2025-09-01T00:00:00Z")
                                .param("end", "2025-09-02T00:00:00Z")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("로그 볼륨 조회 API")
                                .description("기간 동안 로그 볼륨(메트릭 시계열 데이터)을 조회한다.")
                                .responseFields(
                                        fieldString("timestamp", "응답 시간"),
                                        fieldString("status", "응답 상태"),
                                        fieldString("code", "응답 코드"),
                                        fieldString("message", "응답 메시지"),
                                        fieldString("requestId", "요청 ID"),
                                        fieldObject("data.result.data[].metric", "메트릭 key-value 맵"),
                                        fieldString(
                                                "data.result.data[].metric.*",
                                                "메트릭 동적 필드 (예: app, env)"),
                                        fieldNumber(
                                                "data.result.data[].values[].timestamp", "타임스탬프"),
                                        fieldString("data.result.data[].values[].value", "측정 값"))
                                .build());
    }

    @Test
    void getLabels() throws Exception {
        LabelResultDto.LabelsResultDto mockResult =
                LabelResultDto.LabelsResultDto.builder()
                        .result(
                                LabelResultDto.LabelsDto.builder()
                                        .labels(List.of("string", "string"))
                                        .build())
                        .build();

        when(logFacadeService.getLabelResult(any(), any(), any())).thenReturn(mockResult);
        when(requestInfo.getRequestId()).thenReturn("string");

        mockMvc.perform(get("/api/v1/log/labels").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("로그 레이블 조회 API")
                                .description("Loki에서 제공하는 레이블 key 목록을 조회한다.")
                                .responseFields(
                                        fieldString("timestamp", "응답 시간"),
                                        fieldString("status", "응답 상태"),
                                        fieldString("code", "응답 코드"),
                                        fieldString("message", "응답 메시지"),
                                        fieldString("requestId", "요청 ID"),
                                        fieldArray("data.result.labels", "레이블 목록"))
                                .build());
    }

    @Test
    void getLabelValues() throws Exception {
        LabelResultDto.LabelValuesResultDto mockResult =
                LabelResultDto.LabelValuesResultDto.builder()
                        .result(
                                LabelResultDto.LabelValuesDto.builder()
                                        .data(List.of("string", "string"))
                                        .build())
                        .build();

        when(logFacadeService.getLabelValuesResult(anyString(), any(), any(), any(), any()))
                .thenReturn(mockResult);
        when(requestInfo.getRequestId()).thenReturn("string");

        mockMvc.perform(
                        get("/api/v1/log/labels/{label}/values", "app")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("특정 레이블 값 조회 API")
                                .description("특정 레이블 키에 대한 값 목록을 조회한다.")
                                .pathParameters(paramString("label", "레이블 키 (예: app, env 등)"))
                                .responseFields(
                                        fieldString("timestamp", "응답 시간"),
                                        fieldString("status", "응답 상태"),
                                        fieldString("code", "응답 코드"),
                                        fieldString("message", "응답 메시지"),
                                        fieldString("requestId", "요청 ID"),
                                        fieldArray("data.result.data", "레이블 값 목록"))
                                .build());
    }
}
