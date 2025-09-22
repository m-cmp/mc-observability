// package com.mcmp.o11ymanager.manager.controller;
//
// import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;
// import static org.springframework.restdocs.snippet.Attributes.key;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
// import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.AnomalyDetectionMeasurement;
// import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.AnomalyDetectionOptions;
// import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionBody;
// import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionHistory;
// import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionResult;
// import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.LLMChatSession;
// import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.LLMModel;
// import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.Message;
// import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.PostQueryBody;
// import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.PostSessionBody;
// import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.QueryMetadata;
// import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.SessionHistory;
// import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.enums.ProviderType;
// import com.mcmp.o11ymanager.manager.dto.insight.prediction.PredictionMeasurement;
// import com.mcmp.o11ymanager.manager.dto.insight.prediction.PredictionOptions;
// import com.mcmp.o11ymanager.manager.global.vm.ResBody;
// import com.mcmp.o11ymanager.manager.port.InsightPort;
// import com.mcmp.o11ymanager.util.ApiDocumentation;
// import com.mcmp.o11ymanager.util.JsonConverter;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Map;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
// import org.springframework.http.MediaType;
// import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.web.servlet.MockMvc;
//
// @AutoConfigureRestDocs(outputDir = "build/generated-snippets")
// @AutoConfigureMockMvc
// @WebMvcTest(InsightController.class)
// @MockBean(JpaMetamodelMappingContext.class)
// @ActiveProfiles("test")
// class InsightControllerTest {
//
//    @Autowired private MockMvc mockMvc;
//
//    @MockBean private InsightPort insightPort;
//
//    private static final String TAG_PREDICTION = "[Insight] Prediction";
//    private static final String TAG_ANOMALY = "[Insight] AnomalyDetection";
//    private static final String TAG_ALERT = "[Insight] AlertAnalysis";
//    private static final String TAG_LLM = "[Insight] LLM";
//    private static final String TAG_LOG = "[Insight] LogAnalysis";
//
//    /* ===================== ANOMALY ===================== */
//
//    @Test
//    void getAnomalyDetectionMeasurements() throws Exception {
//        AnomalyDetectionMeasurement measurement = new AnomalyDetectionMeasurement();
//        measurement.setPluginSeq(1);
//        measurement.setMeasurement("cpu");
//        measurement.setFields(List.of(Map.of("field_key", "usage_idle", "unit", "percent")));
//
//        ResBody<List<AnomalyDetectionMeasurement>> resBody = new ResBody<>();
//        resBody.setData(List.of(measurement));
//
//        when(insightPort.getMeasurements()).thenReturn(resBody);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//                                "/api/o11y/insight/anomaly-detection/measurement"))
//                .andExpect(status().isOk())
//                .andDo(
//                        builder()
//                                .tag(TAG_ANOMALY)
//                                .description("Retrieve anomaly detection measurement list")
//                                .summary("GetAnomalyDetectionMeasurements")
//                                .responseSchema("AnomalyDetectionMeasurement")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldArray("data", "List of measurements"),
//                                        fieldNumber("data[].pluginSeq", "Plugin sequence")
//                                                .attributes(key("example").value(1)),
//                                        fieldString("data[].measurement", "Measurement name")
//                                                .attributes(key("example").value("cpu")),
//                                        fieldArray("data[].fields", "List of field definitions"),
//                                        fieldString("data[].fields[].field_key", "Field key")
//                                                .attributes(key("example").value("usage_idle")),
//                                        fieldString("data[].fields[].unit", "Unit of measurement")
//                                                .attributes(key("example").value("percent")),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//    }
//
//    @Test
//    void getAnomalyDetectionSpecificMeasurement() throws Exception {
//        AnomalyDetectionMeasurement measurement = new AnomalyDetectionMeasurement();
//        measurement.setPluginSeq(1);
//        measurement.setMeasurement("cpu");
//        measurement.setFields(List.of(Map.of("field_key", "usage_idle", "unit", "percent")));
//
//        when(insightPort.getSpecificMeasurement(eq("cpu"))).thenReturn(new
// ResBody<>(measurement));
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//                                "/api/o11y/insight/anomaly-detection/measurement/{measurement}",
//                                "cpu"))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_ANOMALY)
//                                .description(
//                                        "Retrieve details of a specific anomaly detection
// measurement")
//                                .summary("GetAnomalyDetectionSpecificMeasurement")
//                                .pathParameters(paramString("measurement", "Measurement name"))
//                                .responseSchema("AnomalyDetectionMeasurement")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code (example: 0000)"),
//                                        fieldString(
//                                                "rs_msg", "Response message (example: Success)"),
//                                        fieldObject(
//                                                "data", "Anomaly detection measurement details"),
//                                        fieldNumber(
//                                                "data.pluginSeq", "Plugin sequence (example: 1)"),
//                                        fieldString(
//                                                "data.measurement",
//                                                "Measurement name (example: cpu)"),
//                                        fieldArray("data.fields", "List of field definitions"),
//                                        fieldString(
//                                                "data.fields[].field_key",
//                                                "Field key (example: usage_idle)"),
//                                        fieldString(
//                                                "data.fields[].unit",
//                                                "Unit of measurement (example: percent)"),
//                                        fieldString(
//                                                "error_message",
//                                                "Error message (empty if successful)"))
//                                .build());
//
//        verify(insightPort).getSpecificMeasurement(eq("cpu"));
//    }
//
//    @Test
//    void getAnomalyDetectionOptions() throws Exception {
//        AnomalyDetectionOptions options = new AnomalyDetectionOptions();
//        options.setTargetTypes(List.of("vm", "mci"));
//        options.setMeasurements(List.of("cpu", "mem"));
//        options.setExecutionIntervals(List.of("5m", "10m", "30m"));
//
//        ResBody<AnomalyDetectionOptions> resBody = new ResBody<>();
//        resBody.setData(options);
//
//        when(insightPort.getOptions()).thenReturn(resBody);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//                                "/api/o11y/insight/anomaly-detection/options"))
//                .andExpect(status().isOk())
//                .andDo(
//                        builder()
//                                .tag(TAG_ANOMALY)
//                                .description("Retrieve anomaly detection options")
//                                .summary("GetAnomalyDetectionOptions")
//                                .responseSchema("AnomalyDetectionOptions")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldObject("data", "Options object"),
//                                        fieldArray("data.targetTypes", "Available target types")
//                                                .attributes(
//                                                        key("example").value("[\"vm\",
// \"mci\"]")),
//                                        fieldArray("data.measurements", "Available measurements")
//                                                .attributes(
//                                                        key("example").value("[\"cpu\",
// \"mem\"]")),
//                                        fieldArray("data.executionIntervals", "Execution
// intervals")
//                                                .attributes(
//                                                        key("example")
//                                                                .value(
//                                                                        "[\"5m\", \"10m\",
// \"30m\"]")),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//    }
//
//    /* ===================== PREDICTION ===================== */
//
//    @Test
//    void predictMetric() throws Exception {
//        PredictionBody body = new PredictionBody();
//        body.setTargetType(
//                com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.TargetType.VM);
//        body.setMeasurement(
//                com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.AnomalyMetricType
//                        .CPU);
//
//        PredictionResult result = new PredictionResult();
//        result.setNsId("ns-001");
//        result.setTargetId("vm-123");
//        result.setMeasurement("cpu");
//        result.setTargetType("vm");
//        result.setValues(List.of(Map.of("timestamp", "2024-09-19T00:00:00Z", "value", 88)));
//
//        when(insightPort.predictMetric(eq("ns-001"), eq("vm-123"), any()))
//                .thenReturn(new ResBody<>(result));
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.post(
//
// "/api/o11y/insight/anomaly-detection/nsId/{nsId}/target/{targetId}",
//                                        "ns-001",
//                                        "vm-123")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(JsonConverter.asJsonString(body)))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_ANOMALY)
//                                .description(
//                                        "Execute anomaly detection prediction for a given target")
//                                .summary("PredictMetric")
//                                .pathParameters(
//                                        paramString("nsId", "Namespace ID"),
//                                        paramString("targetId", "Target ID"))
//                                .requestSchema("PredictionBody")
//                                .requestFields(
//                                        fieldEnum(
//                                                "targetType",
//                                                "Target type",
//                                                com.mcmp.o11ymanager.manager.dto.insight
//
// .anomaly_detection.enums.TargetType.class),
//                                        fieldEnum(
//                                                "measurement",
//                                                "Measurement type",
//                                                com.mcmp.o11ymanager.manager.dto.insight
//                                                        .anomaly_detection.enums.AnomalyMetricType
//                                                        .class))
//                                .responseSchema("PredictionResult")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code (example: 0000)"),
//                                        fieldString(
//                                                "rs_msg", "Response message (example: Success)"),
//                                        fieldObject("data", "Prediction result object"),
//                                        fieldString("data.nsId", "Namespace ID")
//                                                .attributes(key("example").value("ns-001")),
//                                        fieldString("data.targetId", "Target ID")
//                                                .attributes(key("example").value("vm-123")),
//                                        fieldString("data.measurement", "Measurement type")
//                                                .attributes(key("example").value("cpu")),
//                                        fieldString("data.targetType", "Target type")
//                                                .attributes(key("example").value("vm")),
//                                        fieldArray("data.values", "Predicted values"),
//                                        fieldString(
//                                                        "data.values[].timestamp",
//                                                        "Prediction timestamp")
//                                                .attributes(
//                                                        key("example")
//                                                                .value("2024-09-19T00:00:00Z")),
//                                        fieldNumber("data.values[].value", "Predicted value")
//                                                .attributes(key("example").value(88)),
//                                        fieldString(
//                                                "error_message",
//                                                "Error message (empty if successful)"))
//                                .build());
//
//        verify(insightPort).predictMetric(eq("ns-001"), eq("vm-123"), any());
//    }
//
//    @Test
//    void getAnomalyDetectionHistory() throws Exception {
//        PredictionHistory history = new PredictionHistory();
//        history.setNsId("ns-001");
//        history.setTargetId("vm-123");
//        history.setMeasurement("cpu");
//        history.setValues(
//                List.of(
//                        Map.of(
//                                "timestamp",
//                                "2024-10-08T06:50:37Z",
//                                "anomaly_score",
//                                0.75,
//                                "is_anomaly",
//                                1,
//                                "value",
//                                85)));
//
//        ResBody<PredictionHistory> resBody = new ResBody<>();
//        resBody.setData(history);
//
//        when(insightPort.getAnomalyHistory(any(), any(), any(), any(),
// any())).thenReturn(resBody);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//
// "/api/o11y/insight/anomaly-detection/nsId/{nsId}/target/{targetId}/history",
//                                        "ns-001",
//                                        "vm-123")
//                                .param("measurement", "cpu"))
//                .andExpect(status().isOk())
//                .andDo(
//                        builder()
//                                .tag(TAG_ANOMALY)
//                                .description("Retrieve anomaly detection history results")
//                                .summary("GetAnomalyDetectionHistory")
//                                .pathParameters(
//                                        paramString("nsId", "Namespace ID"),
//                                        paramString("targetId", "Target ID"))
//                                .queryParameters(paramString("measurement", "Measurement type"))
//                                .responseSchema("PredictionHistory")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldObject("data", "History result"),
//                                        fieldString("data.nsId", "Namespace ID")
//                                                .attributes(key("example").value("ns-001")),
//                                        fieldString("data.targetId", "Target ID")
//                                                .attributes(key("example").value("vm-123")),
//                                        fieldString("data.measurement", "Measurement type")
//                                                .attributes(key("example").value("cpu")),
//                                        fieldArray("data.values", "Anomaly detection results"),
//                                        fieldString("data.values[].timestamp", "Timestamp")
//                                                .attributes(
//                                                        key("example")
//                                                                .value("2024-10-08T06:50:37Z")),
//                                        fieldNumber("data.values[].anomaly_score", "Anomaly
// score")
//                                                .attributes(key("example").value(0.75)),
//                                        fieldNumber("data.values[].is_anomaly", "Is anomaly
// (0/1)")
//                                                .attributes(key("example").value(1)),
//                                        fieldNumber("data.values[].value", "Observed value")
//                                                .attributes(key("example").value(85)),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//
//        verify(insightPort).getAnomalyHistory(any(), any(), any(), any(), any());
//    }
//
//    /* ===================== ALERT ===================== */
//
//    @Test
//    void queryAlertAnalysis() throws Exception {
//        Message mockMessage = new Message();
//        mockMessage.setMessageType("ai");
//        mockMessage.setMessage(
//                "CPU usage spike is related to a memory leak in the application service.");
//        mockMessage.setMetadata(null);
//
//        ResBody<Message> mockResponse = ResBody.success(mockMessage);
//        when(insightPort.queryAlertAnalysis(any(PostQueryBody.class))).thenReturn(mockResponse);
//
//        String requestBody =
//                """
//            {
//              "sessionId": "session_123",
//              "message": "cpu high usage alert"
//            }
//            """;
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.post(
//                                        "/api/o11y/insight/alert-analysis/query")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(requestBody)
//                                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_ALERT)
//                                .description("Send a query to the alert analysis module")
//                                .summary("QueryAlertAnalysis")
//                                .requestSchema("PostQueryBody")
//                                .requestFields(
//                                        fieldString("sessionId", "Session ID"),
//                                        fieldString("message", "User query message"))
//                                .responseSchema("ResBody<Message>")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldObject("data", "Response data object"),
//                                        fieldString(
//                                                "data.messageType",
//                                                "Type of the response message (e.g., ai)"),
//                                        fieldString(
//                                                "data.message", "Content of the response
// message"),
//                                        fieldSubsection(
//                                                        "data.metadata",
//                                                        "Additional query metadata (optional)")
//                                                .optional(),
//                                        fieldString("error_message", "Error message if any"))
//                                .build());
//
//        verify(insightPort).queryAlertAnalysis(any(PostQueryBody.class));
//    }
//
//    /* ===================== PREDICTION ===================== */
//
//    @Test
//    void getPredictionMeasurements() throws Exception {
//        PredictionMeasurement measurement = new PredictionMeasurement();
//        measurement.setPluginSeq(1);
//        measurement.setMeasurement("cpu");
//        measurement.setFields(List.of(Map.of("field_key", "usage_idle", "unit", "percent")));
//
//        when(insightPort.getPredictionMeasurements())
//                .thenReturn(new ResBody<>(List.of(measurement)));
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//                                "/api/o11y/insight/predictions/measurement"))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_PREDICTION)
//                                .description("Retrieve list of measurable prediction metrics")
//                                .summary("GetPredictionMeasurements")
//                                .responseSchema("PredictionMeasurementList")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldArray("data", "List of measurements"),
//                                        fieldNumber("data[].pluginSeq", "Plugin sequence")
//                                                .attributes(key("example").value(1)),
//                                        fieldString("data[].measurement", "Measurement name")
//                                                .attributes(key("example").value("cpu")),
//                                        fieldArray("data[].fields", "List of field definitions"),
//                                        fieldString("data[].fields[].field_key", "Field key")
//                                                .attributes(key("example").value("usage_idle")),
//                                        fieldString("data[].fields[].unit", "Unit of measurement")
//                                                .attributes(key("example").value("percent")),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//    }
//
//    @Test
//    void getPredictionSpecificMeasurement() throws Exception {
//        PredictionMeasurement measurement = new PredictionMeasurement();
//        measurement.setPluginSeq(1);
//        measurement.setMeasurement("cpu");
//        measurement.setFields(List.of(Map.of("field_key", "usage_idle", "unit", "percent")));
//
//        when(insightPort.getPredictionSpecificMeasurement(eq("cpu")))
//                .thenReturn(new ResBody<>(measurement));
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//                                "/api/o11y/insight/predictions/measurement/{measurement}", "cpu"))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_PREDICTION)
//                                .description(
//                                        "Retrieve details of a specific prediction measurement")
//                                .summary("GetPredictionSpecificMeasurement")
//                                .pathParameters(paramString("measurement", "Measurement name"))
//                                .responseSchema("PredictionMeasurementDetail")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code (example: 0000)"),
//                                        fieldString(
//                                                "rs_msg", "Response message (example: Success)"),
//                                        fieldObject("data", "Prediction measurement details"),
//                                        fieldNumber(
//                                                "data.pluginSeq", "Plugin sequence (example: 1)"),
//                                        fieldString(
//                                                "data.measurement",
//                                                "Measurement name (example: cpu)"),
//                                        fieldArray("data.fields", "List of field definitions"),
//                                        fieldString(
//                                                "data.fields[].field_key",
//                                                "Field key (example: usage_idle)"),
//                                        fieldString(
//                                                "data.fields[].unit",
//                                                "Unit of measurement (example: percent)"),
//                                        fieldString(
//                                                "error_message",
//                                                "Error message (empty if successful)"))
//                                .build());
//
//        verify(insightPort).getPredictionSpecificMeasurement(eq("cpu"));
//    }
//
//    @Test
//    void getPredictionOptions() throws Exception {
//        PredictionOptions options = new PredictionOptions();
//        options.setTargetTypes(List.of("vm", "mci"));
//        options.setMeasurements(List.of("cpu", "mem", "disk"));
//        options.setPredictionRanges(Map.of("min", "1h", "max", "2160h"));
//
//        when(insightPort.getPredictionOptions()).thenReturn(new ResBody<>(options));
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//                                "/api/o11y/insight/predictions/options"))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_PREDICTION)
//                                .description("Retrieve prediction configuration options")
//                                .summary("GetPredictionOptions")
//                                .responseSchema("PredictionOptions")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldObject("data", "Prediction options"),
//                                        fieldArray("data.targetTypes", "Available target types")
//                                                .attributes(
//                                                        key("example").value("[\"vm\",
// \"mci\"]")),
//                                        fieldArray("data.measurements", "Available measurements")
//                                                .attributes(
//                                                        key("example")
//                                                                .value(
//                                                                        "[\"cpu\", \"mem\",
// \"disk\"]")),
//                                        fieldObject(
//                                                "data.predictionRanges",
//                                                "Prediction range configuration"),
//                                        fieldString("data.predictionRanges.min", "Minimum range")
//                                                .attributes(key("example").value("1h")),
//                                        fieldString("data.predictionRanges.max", "Maximum range")
//                                                .attributes(key("example").value("2160h")),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//    }
//
//    @Test
//    void getPredictionHistory() throws Exception {
//        PredictionHistory history = new PredictionHistory();
//        history.setNsId("ns-001");
//        history.setTargetId("vm-123");
//        history.setMeasurement("cpu");
//        history.setValues(List.of(Map.of("timestamp", "2024-08-22T00:00:00Z", "value", 55)));
//
//        when(insightPort.getPredictionHistory(eq("ns-001"), eq("vm-123"), eq("cpu"), any(),
// any()))
//                .thenReturn(new ResBody<>(history));
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//
// "/api/o11y/insight/predictions/nsId/{nsId}/target/{targetId}/history",
//                                        "ns-001",
//                                        "vm-123")
//                                .param("measurement", "cpu"))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_PREDICTION)
//                                .description("Retrieve prediction history")
//                                .summary("GetPredictionHistory")
//                                .pathParameters(
//                                        paramString("nsId", "Namespace ID"),
//                                        paramString("targetId", "Target ID"))
//                                .queryParameters(paramString("measurement", "Measurement type"))
//                                .responseSchema("PredictionHistory")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldObject("data", "Prediction history"),
//                                        fieldString("data.nsId", "Namespace ID")
//                                                .attributes(key("example").value("ns-001")),
//                                        fieldString("data.targetId", "Target ID")
//                                                .attributes(key("example").value("vm-123")),
//                                        fieldString("data.measurement", "Measurement type")
//                                                .attributes(key("example").value("cpu")),
//                                        fieldArray("data.values", "Historical prediction values"),
//                                        fieldString("data.values[].timestamp", "History
// timestamp")
//                                                .attributes(
//                                                        key("example")
//                                                                .value("2024-08-22T00:00:00Z")),
//                                        fieldNumber("data.values[].value", "History value")
//                                                .attributes(key("example").value(55)),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//
//        verify(insightPort)
//                .getPredictionHistory(eq("ns-001"), eq("vm-123"), eq("cpu"), any(), any());
//    }
//
//    /* ===================== LLM ===================== */
//
//    @Test
//    void getLLMModelOptions() throws Exception {
//        LLMModel model = new LLMModel();
//        model.setProvider("openai");
//        model.setModelName(List.of("gpt-4", "gpt-3.5"));
//
//        ResBody<List<LLMModel>> mockResponse = ResBody.success(List.of(model));
//        when(insightPort.getLLMModelOptions()).thenReturn(mockResponse);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get("/api/o11y/insight/llm/model")
//                                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_LLM)
//                                .description("Retrieve available LLM model options")
//                                .summary("GetLLMModelOptions")
//                                .responseSchema("ResBody<List<LLMModel>>")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldArray("data", "Available LLM models"),
//                                        fieldString(
//                                                "data[].provider",
//                                                "Provider name (e.g., openai, ollama)"),
//                                        fieldArray("data[].modelName", "List of model names"),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//
//        verify(insightPort).getLLMModelOptions();
//    }
//
//    @Test
//    void getLLMChatSessions() throws Exception {
//        LLMChatSession session = new LLMChatSession();
//        session.setSeq(1);
//        session.setUserId("user1");
//        session.setSessionId("session_123");
//        session.setProvider("openai");
//        session.setModelName("gpt-4");
//        session.setRegdate(LocalDateTime.now());
//
//        ResBody<List<LLMChatSession>> mockResponse = ResBody.success(List.of(session));
//        when(insightPort.getLLMChatSessions()).thenReturn(mockResponse);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get("/api/o11y/insight/llm/session")
//                                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_LLM)
//                                .description("Retrieve all active LLM chat sessions")
//                                .summary("GetLLMChatSessions")
//                                .responseSchema("ResBody<List<LLMChatSession>>")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldArray("data", "Chat sessions list"),
//                                        fieldNumber("data[].seq", "Session sequence ID"),
//                                        fieldString("data[].userId", "User ID"),
//                                        fieldString("data[].sessionId", "Session ID"),
//                                        fieldString("data[].provider", "LLM provider"),
//                                        fieldString("data[].modelName", "LLM model name"),
//                                        fieldString("data[].regdate", "Session creation
// timestamp"),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//
//        verify(insightPort).getLLMChatSessions();
//    }
//
//    @Test
//    void postLLMChatSession() throws Exception {
//        PostSessionBody requestBody = new PostSessionBody();
//        requestBody.setProvider(ProviderType.OPENAI);
//        requestBody.setModelName("gpt-4");
//
//        LLMChatSession session = new LLMChatSession();
//        session.setSeq(1);
//        session.setUserId("user1");
//        session.setSessionId("session_123");
//        session.setProvider("openai");
//        session.setModelName("gpt-4");
//        session.setRegdate(LocalDateTime.now());
//
//        ResBody<LLMChatSession> mockResponse = ResBody.success(session);
//        when(insightPort.postLLMChatSession(any())).thenReturn(mockResponse);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.post("/api/o11y/insight/llm/session")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(JsonConverter.asJsonString(requestBody))
//                                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_LLM)
//                                .description("Create a new LLM chat session")
//                                .summary("PostLLMChatSession")
//                                .requestSchema("PostSessionBody")
//                                .requestFields(
//                                        fieldString("provider", "LLM provider (e.g., OPENAI)"),
//                                        fieldString("modelName", "Model name to use (e.g.,
// gpt-4)"))
//                                .responseSchema("ResBody<LLMChatSession>")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldObject("data", "Created LLM chat session"),
//                                        fieldNumber("data.seq", "Session sequence ID"),
//                                        fieldString("data.userId", "User ID"),
//                                        fieldString("data.sessionId", "Session ID"),
//                                        fieldString("data.provider", "LLM provider"),
//                                        fieldString("data.modelName", "LLM model name"),
//                                        fieldString("data.regdate", "Session creation timestamp"),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//
//        verify(insightPort).postLLMChatSession(any());
//    }
//
//    @Test
//    void deleteLLMChatSession() throws Exception {
//        LLMChatSession mockSession = new LLMChatSession();
//        mockSession.setSeq(1);
//        mockSession.setSessionId("session_123");
//
//        when(insightPort.deleteLLMChatSession(any())).thenReturn(new ResBody<>(mockSession));
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.delete(
//                                        "/api/o11y/insight/llm/session?sessionId={sessionId}",
//                                        "session_123")
//                                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_LLM)
//                                .description("Delete a specific LLM chat session")
//                                .summary("DeleteLLMChatSession")
//                                .queryParameters(paramString("sessionId", "Session ID"))
//                                .responseSchema("ResBody<LLMChatSession>")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldNumber("data.seq", "Session sequence"),
//                                        fieldString("data.userId", "User ID").optional(),
//                                        fieldString("data.sessionId", "Session ID"),
//                                        fieldString("data.provider", "Provider").optional(),
//                                        fieldString("data.modelName", "Model name").optional(),
//                                        fieldString("data.regdate", "Registration
// date").optional(),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//
//        verify(insightPort).deleteLLMChatSession(any());
//    }
//
//    @Test
//    void deleteAllLLMChatSessions() throws Exception {
//        LLMChatSession session = new LLMChatSession();
//        session.setSeq(1);
//        session.setSessionId("session_123");
//
//        ResBody<List<LLMChatSession>> mockResponse = ResBody.success(List.of(session));
//        when(insightPort.deleteAllLLMChatSessions()).thenReturn(mockResponse);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.delete("/api/o11y/insight/llm/sessions")
//                                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_LLM)
//                                .description("Delete all LLM chat sessions")
//                                .summary("DeleteAllLLMChatSessions")
//                                .responseSchema("ResBody<List<LLMChatSession>>")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldArray("data", "Deleted session list"),
//                                        fieldNumber("data[].seq", "Session sequence ID"),
//                                        fieldString("data[].userId", "User ID").optional(),
//                                        fieldString("data[].sessionId", "Deleted session ID"),
//                                        fieldString("data[].provider", "LLM provider").optional(),
//                                        fieldString("data[].modelName", "LLM model name")
//                                                .optional(),
//                                        fieldString("data[].regdate", "Session creation
// timestamp")
//                                                .optional(),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//
//        verify(insightPort).deleteAllLLMChatSessions();
//    }
//
//    @Test
//    void getLLMSessionHistory() throws Exception {
//        Message msg = new Message();
//        msg.setMessageType("user");
//        msg.setMessage("Hello AI");
//        msg.setMetadata(null);
//
//        SessionHistory history = new SessionHistory();
//        history.setSeq(1);
//        history.setUserId("user1");
//        history.setSessionId("session_123");
//        history.setProvider("openai");
//        history.setModelName("gpt-4");
//        history.setRegdate(LocalDateTime.now());
//        history.setMessages(List.of(msg));
//
//        ResBody<SessionHistory> mockResponse = ResBody.success(history);
//        when(insightPort.getLLMSessionHistory(any())).thenReturn(mockResponse);
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.get(
//                                        "/api/o11y/insight/llm/session/{sessionId}/history",
//                                        "session_123")
//                                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_LLM)
//                                .description("Retrieve chat history of a given LLM session")
//                                .summary("GetLLMSessionHistory")
//                                .pathParameters(paramString("sessionId", "Session ID"))
//                                .responseSchema("ResBody<SessionHistory>")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code"),
//                                        fieldString("rs_msg", "Response message"),
//                                        fieldObject("data", "Session history"),
//                                        fieldNumber("data.seq", "Session sequence ID"),
//                                        fieldString("data.userId", "User ID"),
//                                        fieldString("data.sessionId", "Session ID"),
//                                        fieldString("data.provider", "LLM provider"),
//                                        fieldString("data.modelName", "LLM model name"),
//                                        fieldString("data.regdate", "Session creation timestamp"),
//                                        fieldArray("data.messages", "Chat message history"),
//                                        fieldString(
//                                                "data.messages[].messageType",
//                                                "Message type (e.g., user, ai)"),
//                                        fieldString("data.messages[].message", "Message content"),
//                                        fieldObject("data.messages[].metadata", "Message
// metadata")
//                                                .optional(),
//                                        fieldString("error_message", "Error message"))
//                                .build());
//
//        verify(insightPort).getLLMSessionHistory(any());
//    }
//
//    /* ===================== LOG ===================== */
//
//    @Test
//    void queryLogAnalysis() throws Exception {
//        Message mockMessage = new Message();
//        mockMessage.setMessageType("ai");
//        mockMessage.setMessage("Log analysis result: CPU spike detected due to service
// overload.");
//        QueryMetadata metadata = new QueryMetadata();
//        metadata.setQueriesExecuted(List.of("SELECT * FROM logs WHERE level='ERROR'"));
//        metadata.setTotalExecutionTime(1.23);
//        metadata.setToolCallsCount(2);
//        metadata.setDatabasesAccessed(List.of("logs_db", "metrics_db"));
//        mockMessage.setMetadata(metadata);
//
//        when(insightPort.queryLogAnalysis(any())).thenReturn(ResBody.success(mockMessage));
//
//        String requestBody =
//                """
//            {
//              "sessionId": "session_123",
//              "message": "analyze recent error logs"
//            }
//            """;
//
//        mockMvc.perform(
//                        RestDocumentationRequestBuilders.post(
//                                        "/api/o11y/insight/log-analysis/query")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(requestBody)
//                                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andDo(
//                        ApiDocumentation.builder()
//                                .tag(TAG_LOG)
//                                .description("Execute log analysis using LLM")
//                                .summary("Query Log Analysis")
//                                .requestSchema("PostQueryBody")
//                                .requestFields(
//                                        fieldString(
//                                                "sessionId", "Session ID (example: session_123)"),
//                                        fieldString(
//                                                "message",
//                                                "User query (example: analyze recent error
// logs)"))
//                                .responseSchema("Message")
//                                .responseFields(
//                                        fieldString("rs_code", "Response code (example: 0000)"),
//                                        fieldString(
//                                                "rs_msg", "Response message (example: Success)"),
//                                        fieldSubsection("data", "Response data"),
//                                        fieldString(
//                                                "data.messageType",
//                                                "Type of message (example: ai)"),
//                                        fieldString(
//                                                "data.message",
//                                                "Message content (example: Log analysis result:
// CPU spike detected due to service overload.)"),
//                                        fieldArray(
//                                                "data.metadata.queriesExecuted",
//                                                "Executed queries (example: [SELECT * FROM logs
// WHERE level='ERROR'])"),
//                                        fieldNumber(
//                                                "data.metadata.totalExecutionTime",
//                                                "Total execution time in seconds (example:
// 1.23)"),
//                                        fieldNumber(
//                                                "data.metadata.toolCallsCount",
//                                                "Number of tool calls (example: 2)"),
//                                        fieldArray(
//                                                "data.metadata.databasesAccessed",
//                                                "Databases accessed (example: [logs_db,
// metrics_db])"),
//                                        fieldString(
//                                                "error_message",
//                                                "Error message (empty if successful)"))
//                                .build());
//
//        verify(insightPort).queryLogAnalysis(any());
//    }
// }
