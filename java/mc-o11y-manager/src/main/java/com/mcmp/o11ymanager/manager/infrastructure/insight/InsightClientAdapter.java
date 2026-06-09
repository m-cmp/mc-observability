package com.mcmp.o11ymanager.manager.infrastructure.insight;

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
    public Object predictMonitoringDataForInfra(String nsId, String infraId, Object body) {
        return insightClient.predictMonitoringDataForInfra(nsId, infraId, body);
    }

    @Override
    public Object predictMonitoringDataForNode(
            String nsId, String infraId, String nodeId, Object body) {
        return insightClient.predictMonitoringDataForNode(nsId, infraId, nodeId, body);
    }

    @Override
    public Object getPredictionHistoryForInfra(
            String nsId, String infraId, String measurement, String startTime, String endTime) {
        return insightClient.getPredictionHistoryForInfra(
                nsId, infraId, measurement, startTime, endTime);
    }

    @Override
    public Object getPredictionHistoryForNode(
            String nsId,
            String infraId,
            String nodeId,
            String measurement,
            String startTime,
            String endTime) {
        return insightClient.getPredictionHistoryForNode(
                nsId, infraId, nodeId, measurement, startTime, endTime);
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
    public Object predictAnomaly(int settingSeq) {
        return insightClient.predictMetric(settingSeq);
    }

    @Override
    public Object getAnomalySettings() {
        return insightClient.getAnomalySettings();
    }

    @Override
    public Object createAnomalySetting(Object body) {
        return insightClient.createAnomalySetting(body);
    }

    @Override
    public Object updateAnomalySetting(int settingSeq, Object body) {
        return insightClient.updateAnomalySetting(settingSeq, body);
    }

    @Override
    public Object deleteAnomalySetting(int settingSeq) {
        return insightClient.deleteAnomalySetting(settingSeq);
    }

    @Override
    public Object getAnomalySettingsForInfra(String nsId, String infraId) {
        return insightClient.getAnomalySettingsForInfra(nsId, infraId);
    }

    @Override
    public Object getAnomalySettingsForNode(String nsId, String infraId, String nodeId) {
        return insightClient.getAnomalySettingsForNode(nsId, infraId, nodeId);
    }

    @Override
    public Object getAnomalyHistoryForInfra(
            String nsId, String infraId, String measurement, String startTime, String endTime) {
        return insightClient.getAnomalyHistoryForInfra(
                nsId, infraId, measurement, startTime, endTime);
    }

    @Override
    public Object getAnomalyHistoryForNode(
            String nsId,
            String infraId,
            String nodeId,
            String measurement,
            String startTime,
            String endTime) {
        return insightClient.getAnomalyHistoryForNode(
                nsId, infraId, nodeId, measurement, startTime, endTime);
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
        return insightClient.getLLMSessionHistory(sessionId);
    }

    @Override
    public Object getLLMApiKeys(String provider) {
        return insightClient.getLLMApiKeys(provider);
    }

    @Override
    public Object postLLMApiKeys(Object body) {
        return insightClient.postLLMApiKeys(body);
    }

    @Override
    public Object deleteLLMApiKeys(String provider) {
        return insightClient.deleteLLMApiKey(provider);
    }

    /* ===================== Alert Analysis ===================== */
    @Override
    public Object queryAlertAnalysis(Object body) {
        return insightClient.queryAlertAnalysis(body);
    }

    /* ===================== Log Analysis ===================== */
    @Override
    public Object queryLogAnalysis(Object body) {
        return insightClient.queryLogAnalysis(body);
    }

    /* ===================== Server Error Analysis ===================== */
    @Override
    public Object detectServerError(Object body) {
        return insightClient.detectServerError(body);
    }

    @Override
    public Object queryServerError(Object body) {
        return insightClient.queryServerError(body);
    }

    @Override
    public Object listServerErrorRecords(
            String status, String fromDt, String toDt, Integer page, Integer size) {
        return insightClient.listServerErrorRecords(status, fromDt, toDt, page, size);
    }

    @Override
    public Object getServerErrorRecord(int analysisId) {
        return insightClient.getServerErrorRecord(analysisId);
    }

    @Override
    public Object rerunServerErrorAnalysis(int analysisId) {
        return insightClient.rerunServerErrorAnalysis(analysisId);
    }
}
