package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.restdocs.snippet.Attributes.key;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionBody;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionHistory;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionResult;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.PredictionMeasurement;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.PredictionOptions;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.infrastructure.insight.InsightClient;
import com.mcmp.o11ymanager.util.ApiDocumentation;
import com.mcmp.o11ymanager.util.JsonConverter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
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
@WebMvcTest(InsightController.class)
@MockBean(JpaMetamodelMappingContext.class)
@ActiveProfiles("test")
class InsightControllerTest {

  @Autowired
  private MockMvc mockMvc;


  @MockBean
  private InsightClient insightClient;


  private static final String TAG_PREDICTION = "[Insight] Prediction";
  private static final String TAG_ANOMALY = "[Insight] AnomalyDetection";
  private static final String TAG_LLM = "[Insight] LLM";
  private static final String TAG_LOG = "[Insight] LogAnalysis";


  @Test
  void getPredictionMeasurements() throws Exception {
    PredictionMeasurement measurement = new PredictionMeasurement();
    measurement.setPluginSeq(1);
    measurement.setMeasurement("cpu");
    measurement.setFields(List.of(Map.of("field_key", "usage_idle", "unit", "percent")));

    when(insightClient.getPredictionMeasurements())
        .thenReturn(new ResBody<>(List.of(measurement)));

    mockMvc.perform(
            RestDocumentationRequestBuilders.get("/api/o11y/insight/predictions/measurement"))
        .andExpect(status().isOk())
        .andDo(ApiDocumentation.builder()
            .tag(TAG_PREDICTION)
            .description("예측 가능한 measurement 목록 조회")
            .summary("GetPredictionMeasurements")
            .responseSchema("PredictionMeasurement")
            .responseFields(
                fieldString("rs_code", "응답 코드"),
                fieldString("rs_msg", "응답 메시지"),
                fieldArray("data", "Measurement 목록"),
                fieldNumber("data[].pluginSeq", "Plugin sequence")
                    .attributes(key("example").value(1)),
                fieldString("data[].measurement", "Measurement name")
                    .attributes(key("example").value("cpu")),
                fieldArray("data[].fields", "List of field definitions"),
                fieldString("data[].fields[].field_key", "Field key")
                    .attributes(key("example").value("usage_idle")),
                fieldString("data[].fields[].unit", "Unit of measurement")
                    .attributes(key("example").value("percent")),
                fieldString("error_message", "에러 메시지")
            )
            .build());
  }

  @Test
  void getPredictionOptions() throws Exception {
    PredictionOptions options = new PredictionOptions();
    options.setTargetTypes(List.of("vm", "mci"));
    options.setMeasurements(List.of("cpu", "mem", "disk"));
    options.setPredictionRanges(Map.of("min", "1h", "max", "2160h"));

    when(insightClient.getPredictionOptions()).thenReturn(new ResBody<>(options));

    mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/insight/predictions/options"))
        .andExpect(status().isOk())
        .andDo(ApiDocumentation.builder()
            .tag(TAG_PREDICTION)
            .description("예측 옵션 조회")
            .summary("GetPredictionOptions")
            .responseSchema("PredictionOptions")
            .responseFields(
                fieldString("rs_code", "응답 코드"),
                fieldString("rs_msg", "응답 메시지"),
                fieldObject("data", "Prediction Options"),
                fieldArray("data.targetTypes", "Available target types")
                    .attributes(key("example").value("[\"vm\", \"mci\"]")),
                fieldArray("data.measurements", "Available measurements")
                    .attributes(key("example").value("[\"cpu\", \"mem\", \"disk\"]")),
                fieldObject("data.predictionRanges", "Prediction range configuration"),
                fieldString("data.predictionRanges.min", "Minimum range")
                    .attributes(key("example").value("1h")),
                fieldString("data.predictionRanges.max", "Maximum range")
                    .attributes(key("example").value("2160h")),
                fieldString("error_message", "에러 메시지")
            )
            .build());
  }

  @Test
  void predictMonitoringData() throws Exception {
    PredictionBody body = new PredictionBody();
    body.setTargetType(com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.TargetType.VM);
    body.setMeasurement(
        com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.AnomalyMetricType.CPU);

    PredictionResult result = new PredictionResult();
    result.setNsId("ns-001");
    result.setTargetId("vm-123");
    result.setMeasurement("cpu");
    result.setTargetType("vm");
    result.setValues(List.of(Map.of("timestamp", "2024-08-22T00:00:00Z", "value", 75)));

    when(insightClient.predictMonitoringData(eq("ns-001"), eq("vm-123"), any()))
        .thenReturn(new ResBody<>(result));

    mockMvc.perform(RestDocumentationRequestBuilders.post(
                "/api/o11y/insight/predictions/nsId/{nsId}/target/{targetId}",
                "ns-001", "vm-123")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonConverter.asJsonString(body)))
        .andExpect(status().isOk())
        .andDo(ApiDocumentation.builder()
            .tag(TAG_PREDICTION)
            .description("Metric 예측 실행")
            .summary("PredictMonitoringData")
            .pathParameters(
                paramString("nsId", "Namespace ID"),
                paramString("targetId", "Target ID"))
            .requestSchema("PredictionBody")
            .requestFields(
                fieldEnum("targetType", "Target type",
                    com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.TargetType.class),
                fieldEnum("measurement", "Measurement type",
                    com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.AnomalyMetricType.class)
            )
            .responseSchema("PredictionResult")
            .responseFields(
                fieldString("rs_code", "응답 코드"),
                fieldString("rs_msg", "응답 메시지"),
                fieldObject("data", "Prediction result"),
                fieldString("data.nsId", "Namespace ID")
                    .attributes(key("example").value("ns-001")),
                fieldString("data.targetId", "Target ID")
                    .attributes(key("example").value("vm-123")),
                fieldString("data.measurement", "Measurement type")
                    .attributes(key("example").value("cpu")),
                fieldString("data.targetType", "Target type")
                    .attributes(key("example").value("vm")),
                fieldArray("data.values", "Predicted values"),
                fieldString("data.values[].timestamp", "Prediction timestamp")
                    .attributes(key("example").value("2024-08-22T00:00:00Z")),
                fieldNumber("data.values[].value", "Predicted value")
                    .attributes(key("example").value(75)),
                fieldString("error_message", "에러 메시지")
            )
            .build());
  }

  @Test
  void getPredictionHistory() throws Exception {
    PredictionHistory history = new PredictionHistory();
    history.setNsId("ns-001");
    history.setTargetId("vm-123");
    history.setMeasurement("cpu");
    history.setValues(List.of(Map.of("timestamp", "2024-08-22T00:00:00Z", "value", 55)));

    when(insightClient.getPredictionHistory(eq("ns-001"), eq("vm-123"), eq("cpu"), any(), any()))
        .thenReturn(new ResBody<>(history));

    mockMvc.perform(RestDocumentationRequestBuilders.get(
                "/api/o11y/insight/predictions/nsId/{nsId}/target/{targetId}/history",
                "ns-001", "vm-123")
            .param("measurement", "cpu"))
        .andExpect(status().isOk())
        .andDo(ApiDocumentation.builder()
            .tag(TAG_PREDICTION)
            .description("Prediction 히스토리 조회")
            .summary("GetPredictionHistory")
            .pathParameters(
                paramString("nsId", "Namespace ID"),
                paramString("targetId", "Target ID"))
            .queryParameters(paramString("measurement", "Measurement type"))
            .responseSchema("PredictionHistory")
            .responseFields(
                fieldString("rs_code", "응답 코드"),
                fieldString("rs_msg", "응답 메시지"),
                fieldObject("data", "Prediction history"),
                fieldString("data.nsId", "Namespace ID")
                    .attributes(key("example").value("ns-001")),
                fieldString("data.targetId", "Target ID")
                    .attributes(key("example").value("vm-123")),
                fieldString("data.measurement", "Measurement type")
                    .attributes(key("example").value("cpu")),
                fieldArray("data.values", "Historical prediction values"),
                fieldString("data.values[].timestamp", "History timestamp")
                    .attributes(key("example").value("2024-08-22T00:00:00Z")),
                fieldNumber("data.values[].value", "History value")
                    .attributes(key("example").value(55)),
                fieldString("error_message", "에러 메시지")
            )
            .build());
  }


}
