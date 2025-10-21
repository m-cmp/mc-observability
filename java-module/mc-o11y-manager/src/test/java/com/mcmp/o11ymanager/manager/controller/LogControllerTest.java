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
                                .param("direction", "FORWARD")
                                .param("interval", "1m")
                                .param("step", "30s")
                                .param("since", "1h")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("LogRangeQuery")
                                .description(
                                        "Retrieve log data for a specific query within a given time range.")
                                .queryParameters(
                                        paramString(
                                                "query", "Query string (e.g., {NS_ID=\"test01\"})"),
                                        paramString(
                                                "start",
                                                "Start timestamp (RFC3339) (e.g., 2025-09-01T00:00:00Z)"),
                                        paramString(
                                                "end",
                                                "End timestamp (RFC3339) (e.g., 2025-09-02T00:00:00Z)"),
                                        paramString("limit", "Maximum number of entries"),
                                        paramString("direction", "Direction (FORWARD/BACKWARD)")
                                                .optional(),
                                        paramString("interval", "Interval (e.g. 1m) ").optional(),
                                        paramString("step", "Step (e.g. 30s) ").optional(),
                                        paramString("since", "Since duration (e.g. 1h) ")
                                                .optional())
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"),
                                        fieldObject("data", "Log result object").optional(),
                                        fieldString("data.status", "Log status (e.g., 'string')")
                                                .optional(),
                                        fieldObject("data.data[].labels", "Log labels").optional(),
                                        fieldString(
                                                        "data.data[].labels.*",
                                                        "Label key-value (dynamic, e.g., app:'nginx')")
                                                .optional(),
                                        fieldNumber("data.data[].timestamp", "Timestamp (e.g., 0)")
                                                .optional(),
                                        fieldString(
                                                        "data.data[].value",
                                                        "Log value (e.g., 'message')")
                                                .optional(),
                                        fieldNumber(
                                                        "data.stats.totalBytesProcessed",
                                                        "Total bytes processed (e.g., 0)")
                                                .optional(),
                                        fieldNumber(
                                                        "data.stats.totalLinesProcessed",
                                                        "Total lines processed (e.g., 0)")
                                                .optional(),
                                        fieldNumber(
                                                        "data.stats.execTime",
                                                        "Execution time (ms) (e.g., 0.0)")
                                                .optional(),
                                        fieldNumber(
                                                        "data.stats.totalEntriesReturned",
                                                        "Total entries returned (e.g., 0)")
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
                                .param("limit", "5")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("LogVolumeQuery")
                                .description(
                                        "Retrieve log volumes (metric time series data) for the given period.")
                                .queryParameters(
                                        paramString(
                                                "query", "Query string (e.g., {NS_ID=\"test01\"})"),
                                        paramString(
                                                "start",
                                                "Start timestamp (RFC3339) (e.g., 2025-09-01T00:00:00Z)"),
                                        paramString(
                                                "end",
                                                "End timestamp (RFC3339) (e.g., 2025-09-02T00:00:00Z)"),
                                        paramString("limit", "Maximum series returned").optional())
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"),
                                        fieldObject("data.data[].metric", "Metric key-value map"),
                                        fieldString(
                                                "data.data[].metric.*",
                                                "Metric dynamic fields (e.g., app:'string')"),
                                        fieldNumber(
                                                "data.data[].values[].timestamp",
                                                "Timestamp (e.g., 0)"),
                                        fieldString(
                                                "data.data[].values[].value",
                                                "Measured value (e.g., 'string')"))
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

        mockMvc.perform(
                        get("/api/o11y/log/labels")
                                .param("start", "2025-09-01T00:00:00Z")
                                .param("end", "2025-09-02T00:00:00Z")
                                .param("query", "test-query")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("LogLabelsQuery")
                                .description("Retrieve the list of label keys provided by Loki.")
                                .queryParameters(
                                        paramString(
                                                        "start",
                                                        "Start timestamp (RFC3339) (e.g., 2025-09-01T00:00:00Z)")
                                                .optional(),
                                        paramString(
                                                        "end",
                                                        "End timestamp (RFC3339) (e.g., 2025-09-02T00:00:00Z)")
                                                .optional(),
                                        paramString(
                                                        "query",
                                                        "Query string (e.g., {NS_ID=\"test01\"})")
                                                .optional())
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"),
                                        fieldArray(
                                                "data.labels",
                                                "List of labels (e.g., ['app','env'])"))
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
                                .param("start", "2025-09-01T00:00:00Z")
                                .param("end", "2025-09-02T00:00:00Z")
                                .param("since", "1h")
                                .param("query", "test-query")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .summary("LabelValueQuery")
                                .description(
                                        "Retrieve the list of values for a specific label key.")
                                .pathParameters(
                                        paramString(
                                                "label",
                                                "Label key (e.g., NS_ID, MCI_ID, service)"))
                                .queryParameters(
                                        paramString(
                                                        "start",
                                                        "Start timestamp (RFC3339) (e.g., 2025-09-01T00:00:00Z)")
                                                .optional()
                                                .optional(),
                                        paramString(
                                                        "end",
                                                        "End timestamp (RFC3339) (e.g., 2025-09-02T00:00:00Z)")
                                                .optional()
                                                .optional(),
                                        paramString("since", "Since duration (e.g., 1h)")
                                                .optional(),
                                        paramString(
                                                        "query",
                                                        "Query string (e.g., {NS_ID=\"test01\"})")
                                                .optional()
                                                .optional())
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"),
                                        fieldArray(
                                                "data.data",
                                                "List of label values (e.g., ['string','string'])"))
                                .build());
    }
}
