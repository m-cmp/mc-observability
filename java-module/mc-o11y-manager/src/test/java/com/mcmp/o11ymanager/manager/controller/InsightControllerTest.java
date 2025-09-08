package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.manager.infrastructure.insight.InsightClient;
import com.mcmp.o11ymanager.util.ApiDocumentation;
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
    private static final String TAG = "[Insight] Prediction";

    @Autowired private MockMvc mockMvc;
    @MockBean private InsightClient insightClient;

    @Test
    void getPredictionMeasurement() throws Exception {
        Object mockData = java.util.Map.of("measurement", "string");
        when(insightClient.getPredictionMeasurement()).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/predictions/measurement")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("예측 측정값 목록 조회")
                                .summary("GetPredictionMeasurement")
                                .responseSchema("Object")
                                .responseFields(fieldString("measurement", "측정값 이름"))
                                .build());
        verify(insightClient).getPredictionMeasurement();
    }

    @Test
    void getPredictionSpecificMeasurement() throws Exception {
        Object mockData = java.util.Map.of("measurement", "string");
        when(insightClient.getPredictionSpecificMeasurement(any())).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/predictions/measurement/{measurement}",
                                        "cpu")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("특정 예측 측정값 조회")
                                .summary("GetPredictionSpecificMeasurement")
                                .pathParameters(paramString("measurement", "측정값 이름"))
                                .responseSchema("Object")
                                .responseFields(fieldString("measurement", "측정값 이름"))
                                .build());
        verify(insightClient).getPredictionSpecificMeasurement(any());
    }

    @Test
    void getPredictionOptions() throws Exception {
        Object mockData = java.util.Map.of("option", "string");
        when(insightClient.getPredictionOptions()).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/predictions/options")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("예측 옵션 목록 조회")
                                .summary("GetPredictionOptions")
                                .responseSchema("Object")
                                .responseFields(fieldString("option", "옵션 값"))
                                .build());
        verify(insightClient).getPredictionOptions();
    }

    @Test
    void getPredictionHistory() throws Exception {
        Object mockData = java.util.Map.of("history", "string");
        when(insightClient.getPredictionHistory(any(), any(), any(), any(), any()))
                .thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/predictions/nsId/{nsId}/vm/{vmId}/history",
                                        "ns1",
                                        "vm1")
                                .param("measurement", "cpu")
                                .param("start_time", "2023-01-01T00:00:00Z")
                                .param("end_time", "2023-01-02T00:00:00Z")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("예측 이력 조회")
                                .summary("GetPredictionHistory")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("vmId", "TARGET ID"))
                                .queryParameters(
                                        paramString("measurement", "측정값 이름"),
                                        paramString("start_time", "시작 시간"),
                                        paramString("end_time", "종료 시간"))
                                .responseSchema("Object")
                                .responseFields(fieldString("history", "예측 이력"))
                                .build());
        verify(insightClient).getPredictionHistory(any(), any(), any(), any(), any());
    }

    @Test
    void predictMetric() throws Exception {
        Object mockData = java.util.Map.of("result", "string");
        when(insightClient.predictMetric(any(), any(), any())).thenReturn(mockData);

        String requestBody = "{\"option\":\"value\"}";
        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/insight/insight/predictions/nsId/{nsId}/vm/{vmId}",
                                        "ns1",
                                        "vm1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("예측 실행")
                                .summary("PredictMetric")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("vmId", "TARGET ID"))
                                .requestSchema("Object")
                                .requestFields(fieldString("option", "옵션 값"))
                                .responseSchema("Object")
                                .responseFields(fieldString("result", "예측 결과"))
                                .build());
        verify(insightClient).predictMetric(any(), any(), any());
    }

    @Test
    void getAnomalyDetectionMeasurement() throws Exception {
        Object mockData = java.util.Map.of("measurement", "string");
        when(insightClient.getAnomalyDetectionMeasurement()).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/anomaly-detection/measurement")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("이상탐지 측정값 목록 조회")
                                .summary("GetAnomalyDetectionMeasurement")
                                .responseSchema("Object")
                                .responseFields(fieldString("measurement", "측정값 이름"))
                                .build());
        verify(insightClient).getAnomalyDetectionMeasurement();
    }

    @Test
    void getAnomalyDetectionSpecificMeasurement() throws Exception {
        Object mockData = java.util.Map.of("measurement", "string");
        when(insightClient.getAnomalyDetectionSpecificMeasurement(any())).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/anomaly-detection/measurement/{measurement}",
                                        "cpu")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("특정 이상탐지 측정값 조회")
                                .summary("GetAnomalyDetectionSpecificMeasurement")
                                .pathParameters(paramString("measurement", "측정값 이름"))
                                .responseSchema("Object")
                                .responseFields(fieldString("measurement", "측정값 이름"))
                                .build());
        verify(insightClient).getAnomalyDetectionSpecificMeasurement(any());
    }

    @Test
    void getAnomalyDetectionOptions() throws Exception {
        Object mockData = java.util.Map.of("option", "string");
        when(insightClient.getAnomalyDetectionOptions()).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/anomaly-detection/options")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("이상탐지 옵션 목록 조회")
                                .summary("GetAnomalyDetectionOptions")
                                .responseSchema("Object")
                                .responseFields(fieldString("option", "옵션 값"))
                                .build());
        verify(insightClient).getAnomalyDetectionOptions();
    }

    @Test
    void getAnomalyDetectionSettings() throws Exception {
        Object mockData = java.util.Map.of("settings", "string");
        when(insightClient.getAnomalyDetectionSettings()).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/anomaly-detection/settings")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("이상탐지 설정 목록 조회")
                                .summary("GetAnomalyDetectionSettings")
                                .responseSchema("Object")
                                .responseFields(fieldString("settings", "설정 값"))
                                .build());
        verify(insightClient).getAnomalyDetectionSettings();
    }

    @Test
    void insertAnomalyDetectionSetting() throws Exception {
        Object mockData = java.util.Map.of("inserted", true);
        when(insightClient.insertAnomalyDetectionSetting(any())).thenReturn(mockData);

        String requestBody = "{\"setting\":\"value\"}";
        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/insight/insight/anomaly-detection/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("이상탐지 설정 추가")
                                .summary("InsertAnomalyDetectionSetting")
                                .requestSchema("Object")
                                .requestFields(fieldString("setting", "설정 값"))
                                .responseSchema("Object")
                                .responseFields(fieldBoolean("inserted", "추가 성공 여부"))
                                .build());
        verify(insightClient).insertAnomalyDetectionSetting(any());
    }

    @Test
    void updateAnomalyDetectionSetting() throws Exception {
        Object mockData = java.util.Map.of("updated", true);
        when(insightClient.updateAnomalyDetectionSetting(any(), any())).thenReturn(mockData);

        String requestBody = "{\"setting\":\"value\"}";
        mockMvc.perform(
                        RestDocumentationRequestBuilders.put(
                                        "/api/o11y/insight/insight/anomaly-detection/settings/{settingSeq}",
                                        1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("이상탐지 설정 수정")
                                .summary("UpdateAnomalyDetectionSetting")
                                .pathParameters(paramNumber("settingSeq", "설정 시퀀스"))
                                .requestSchema("Object")
                                .requestFields(fieldString("setting", "설정 값"))
                                .responseSchema("Object")
                                .responseFields(fieldBoolean("updated", "수정 성공 여부"))
                                .build());
        verify(insightClient).updateAnomalyDetectionSetting(any(), any());
    }

    @Test
    void deleteAnomalyDetectionSetting() throws Exception {
        Object mockData = java.util.Map.of("deleted", true);
        when(insightClient.deleteAnomalyDetectionSetting(any())).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete(
                                        "/api/o11y/insight/insight/anomaly-detection/settings/{settingSeq}",
                                        1L)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("이상탐지 설정 삭제")
                                .summary("DeleteAnomalyDetectionSetting")
                                .pathParameters(paramNumber("settingSeq", "설정 시퀀스"))
                                .responseSchema("Object")
                                .responseFields(fieldBoolean("deleted", "삭제 성공 여부"))
                                .build());
        verify(insightClient).deleteAnomalyDetectionSetting(any());
    }

    @Test
    void getAnomalyDetection() throws Exception {
        Object mockData = java.util.Map.of("detection", "string");
        when(insightClient.getAnomalyDetection(any(), any())).thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/anomaly-detection/settings/nsId/{nsId}/vm/{vmId}",
                                        "ns1",
                                        "vm1")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("이상탐지 결과 조회")
                                .summary("GetAnomalyDetection")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("vmId", "TARGET ID"))
                                .responseSchema("Object")
                                .responseFields(fieldString("detection", "이상탐지 결과"))
                                .build());
        verify(insightClient).getAnomalyDetection(any(), any());
    }

    @Test
    void getAnomalyDetectionHistory() throws Exception {
        Object mockData = java.util.Map.of("history", "string");
        when(insightClient.getAnomalyDetectionHistory(any(), any(), any(), any(), any()))
                .thenReturn(mockData);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/insight/insight/anomaly-detection/nsId/{nsId}/vm/{vmId}/history",
                                        "ns1",
                                        "vm1")
                                .param("measurement", "cpu")
                                .param("start_time", "2023-01-01T00:00:00Z")
                                .param("end_time", "2023-01-02T00:00:00Z")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("이상탐지 이력 조회")
                                .summary("GetAnomalyDetectionHistory")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("vmId", "TARGET ID"))
                                .queryParameters(
                                        paramString("measurement", "측정값 이름"),
                                        paramString("start_time", "시작 시간"),
                                        paramString("end_time", "종료 시간"))
                                .responseSchema("Object")
                                .responseFields(fieldString("history", "이상탐지 이력"))
                                .build());
        verify(insightClient).getAnomalyDetectionHistory(any(), any(), any(), any(), any());
    }
}
