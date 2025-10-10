package com.mcmp.o11ymanager.manager.infrastructure.insight;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.*;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.*;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.*;
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
    public Object getPredictionMeasurements() {
        return insightClient.getPredictionMeasurements();
    }

    @Override
    public Object getPredictionSpecificMeasurement(String measurement) {
        return insightClient.getPredictionSpecificMeasurement(measurement);
    }

    @Override
    public Object getPredictionOptions() {
        return insightClient.getPredictionOptions();
    }

    @Override
    public Object predictMonitoringData(
            String nsId, String vmId, Object body) {
        return insightClient.predictMonitoringData(body);
    }

    @Override
    public Object getPredictionHistory(
            String nsId, String vmId, String measurement, String startTime, String endTime) {
        return insightClient.getPredictionHistory(nsId, vmId, measurement, startTime, endTime);
    }

    /* ===================== Anomaly Detection ===================== */
    @Override
    public Object getMeasurements() {
        return insightClient.getMeasurements();
    }

    @Override
    public Object getSpecificMeasurement(String measurement) {
        return insightClient.getSpecificMeasurement(measurement);
    }

    @Override
    public Object getOptions() {
        return insightClient.getOptions();
    }

    @Override
    public Object predictMetric(
            String nsId, String targetId, Object body) {
        return insightClient.predictMetric(nsId, targetId, body);
    }

    @Override
    public Object getAnomalyHistory(
            String nsId, String targetId, String measurement, String startTime, String endTime) {
        return insightClient.getAnomalyHistory(nsId, targetId, measurement, startTime, endTime);
    }

    /* ===================== LLM ===================== */
    @Override
    public Object getLLMModelOptions() {
        return insightClient.getLLMModelOptions();
    }

    @Override
    public Object getLLMChatSessions() {
        return insightClient.getLLMChatSessions();
    }

    @Override
    public Object postLLMChatSession(Object body) {
        return insightClient.postLLMChatSession(body);
    }

    @Override
    public Object deleteLLMChatSession(String sessionId) {
        return insightClient.deleteLLMChatSession(sessionId);
    }

    @Override
    public Object deleteAllLLMChatSessions() {
        return insightClient.deleteAllLLMChatSessions();
    }

    @Override
    public Object getLLMSessionHistory(String sessionId) {
        return insightClient.getLLMSessionHistory();
    }

    /* ===================== Alert Analysis ===================== */
    @Override
    public Object queryAlertAnalysis(Object body) {
        return insightClient.queryAlertAnalysis(body);
    }

    /* ===================== Log Analysis ===================== */
    @Override
    public Object queryLogAnalysis(Object body) {
        return insightClient.queryLogAnalysis();
    }
}
