package com.mcmp.o11ymanager.manager.infrastructure.insight;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.*;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.*;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.*;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.port.InsightPort;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InsightClientAdapter implements InsightPort {

    private final InsightClient insightClient;

    /* ===================== Prediction ===================== */
    @Override
    public ResBody<List<PredictionMeasurement>> getPredictionMeasurements() {
        return insightClient.getPredictionMeasurements();
    }

    @Override
    public ResBody<PredictionMeasurement> getPredictionSpecificMeasurement(String measurement) {
        return insightClient.getPredictionSpecificMeasurement(measurement);
    }

    @Override
    public ResBody<PredictionOptions> getPredictionOptions() {
        return insightClient.getPredictionOptions();
    }

    @Override
    public ResBody<PredictionResult> predictMonitoringData(
            String nsId, String vmId, PredictionBody body) {
        return insightClient.predictMonitoringData(nsId, vmId, body);
    }

    @Override
    public ResBody<PredictionHistory> getPredictionHistory(
            String nsId, String vmId, String measurement, String startTime, String endTime) {
        return insightClient.getPredictionHistory(nsId, vmId, measurement, startTime, endTime);
    }

    /* ===================== Anomaly Detection ===================== */
    @Override
    public ResBody<List<AnomalyDetectionMeasurement>> getMeasurements() {
        return insightClient.getMeasurements();
    }

    @Override
    public ResBody<AnomalyDetectionMeasurement> getSpecificMeasurement(String measurement) {
        return insightClient.getSpecificMeasurement(measurement);
    }

    @Override
    public ResBody<AnomalyDetectionOptions> getOptions() {
        return insightClient.getOptions();
    }

    @Override
    public ResBody<PredictionResult> predictMetric(
            String nsId, String targetId, PredictionBody body) {
        return insightClient.predictMetric(nsId, targetId, body);
    }

    @Override
    public ResBody<PredictionHistory> getAnomalyHistory(
            String nsId, String targetId, String measurement, String startTime, String endTime) {
        return insightClient.getAnomalyHistory(nsId, targetId, measurement, startTime, endTime);
    }

    /* ===================== LLM ===================== */
    @Override
    public ResBody<List<LLMModel>> getLLMModelOptions() {
        return insightClient.getLLMModelOptions();
    }

    @Override
    public ResBody<List<LLMChatSession>> getLLMChatSessions() {
        return insightClient.getLLMChatSessions();
    }

    @Override
    public ResBody<LLMChatSession> postLLMChatSession(PostSessionBody body) {
        return insightClient.postLLMChatSession(body);
    }

    @Override
    public ResBody<LLMChatSession> deleteLLMChatSession(String sessionId) {
        return insightClient.deleteLLMChatSession(sessionId);
    }

    @Override
    public ResBody<List<LLMChatSession>> deleteAllLLMChatSessions() {
        return insightClient.deleteAllLLMChatSessions();
    }

    @Override
    public ResBody<SessionHistory> getLLMSessionHistory(String sessionId) {
        return insightClient.getLLMSessionHistory(sessionId);
    }

    /* ===================== Alert Analysis ===================== */
    @Override
    public ResBody<Message> queryAlertAnalysis(PostQueryBody body) {
        return insightClient.queryAlertAnalysis(body);
    }

    /* ===================== Log Analysis ===================== */
    @Override
    public ResBody<Message> queryLogAnalysis(PostQueryBody body) {
        return insightClient.queryLogAnalysis(body);
    }
}
