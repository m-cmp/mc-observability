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

    private static final String TAG = "[Manager] Monitoring Log";

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
                        get("/api/o11y/log/query_range")
                                .param("query", "test-query")
                                .param("start", "2025-09-01T00:00:00Z")
                                .param("end", "2025-09-02T00:00:00Z")
                                .param("limit", "10")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("LogRangeQuery")
                                .description(
                                        "Retrieve log data for a specific query within a given time range.")
                                .responseFields(
                                        fieldString("timestamp", "Response timestamp"),
                                        fieldString("status", "Response status"),
                                        fieldString("code", "Response code"),
                                        fieldString("message", "Response message"),
                                        fieldString("requestId", "Request ID"),
                                        fieldObject("data.result", "Log result object").optional(),
                                        fieldString("data.result.status", "Log status").optional(),
                                        fieldObject("data.result.data[].labels", "Log labels")
                                                .optional(),
                                        fieldString(
                                                        "data.result.data[].labels.*",
                                                        "Label key-value (dynamic)")
                                                .optional(),
                                        fieldNumber("data.result.data[].timestamp", "Timestamp")
                                                .optional(),
                                        fieldString("data.result.data[].value", "Log value")
                                                .optional(),
                                        fieldNumber(
                                                        "data.result.stats.totalBytesProcessed",
                                                        "Total bytes processed")
                                                .optional(),
                                        fieldNumber(
                                                        "data.result.stats.totalLinesProcessed",
                                                        "Total lines processed")
                                                .optional(),
                                        fieldNumber(
                                                        "data.result.stats.execTime",
                                                        "Execution time (ms)")
                                                .optional(),
                                        fieldNumber(
                                                        "data.result.stats.totalEntriesReturned",
                                                        "Total entries returned")
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
                        get("/api/o11y/log/log_volumes")
                                .param("query", "test-query")
                                .param("start", "2025-09-01T00:00:00Z")
                                .param("end", "2025-09-02T00:00:00Z")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("LogVolumeQuery")
                                .description(
                                        "Retrieve log volumes (metric time series data) for the given period.")
                                .responseFields(
                                        fieldString("timestamp", "Response timestamp"),
                                        fieldString("status", "Response status"),
                                        fieldString("code", "Response code"),
                                        fieldString("message", "Response message"),
                                        fieldString("requestId", "Request ID"),
                                        fieldObject(
                                                "data.result.data[].metric",
                                                "Metric key-value map"),
                                        fieldString(
                                                "data.result.data[].metric.*",
                                                "Metric dynamic fields (e.g., app, env)"),
                                        fieldNumber(
                                                "data.result.data[].values[].timestamp",
                                                "Timestamp"),
                                        fieldString(
                                                "data.result.data[].values[].value",
                                                "Measured value"))
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

        mockMvc.perform(get("/api/o11y/log/labels").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("LogLabelsQuery")
                                .description("Retrieve the list of label keys provided by Loki.")
                                .responseFields(
                                        fieldString("timestamp", "Response timestamp"),
                                        fieldString("status", "Response status"),
                                        fieldString("code", "Response code"),
                                        fieldString("message", "Response message"),
                                        fieldString("requestId", "Request ID"),
                                        fieldArray("data.result.labels", "List of labels"))
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
                        get("/api/o11y/log/labels/{label}/values", "app")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("LabelValueQuery")
                                .description(
                                        "Retrieve the list of values for a specific label key.")
                                .pathParameters(paramString("label", "Label key (e.g., app, env)"))
                                .responseFields(
                                        fieldString("timestamp", "Response timestamp"),
                                        fieldString("status", "Response status"),
                                        fieldString("code", "Response code"),
                                        fieldString("message", "Response message"),
                                        fieldString("requestId", "Request ID"),
                                        fieldArray("data.result.data", "List of label values"))
                                .build());
    }
}
