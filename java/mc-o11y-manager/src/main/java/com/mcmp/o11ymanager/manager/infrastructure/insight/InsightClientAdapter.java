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
    public Object predictMonitoringDataForMci(String nsId, String mciId, Object body) {
        return insightClient.predictMonitoringDataForMci(nsId, mciId, body);
    }

    @Override
    public Object predictMonitoringDataForVm(String nsId, String mciId, String vmId, Object body) {
        return insightClient.predictMonitoringDataForVm(nsId, mciId, vmId, body);
    }

    @Override
    public Object getPredictionHistoryForMci(
            String nsId, String mciId, String measurement, String startTime, String endTime) {
        return insightClient.getPredictionHistoryForMci(
                nsId, mciId, measurement, startTime, endTime);
    }

    @Override
    public Object getPredictionHistoryForVm(
            String nsId,
            String mciId,
            String vmId,
            String measurement,
            String startTime,
            String endTime) {
        return insightClient.getPredictionHistoryForVm(
                nsId, mciId, vmId, measurement, startTime, endTime);
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
    public Object getAnomalySettingsForMci(String nsId, String mciId) {
        return insightClient.getAnomalySettingsForMci(nsId, mciId);
    }

    @Override
    public Object getAnomalySettingsForVm(String nsId, String mciId, String vmId) {
        return insightClient.getAnomalySettingsForVm(nsId, mciId, vmId);
    }

    @Override
    public Object getAnomalyHistoryForMci(
            String nsId, String mciId, String measurement, String startTime, String endTime) {
        return insightClient.getAnomalyHistoryForMci(nsId, mciId, measurement, startTime, endTime);
    }

    @Override
    public Object getAnomalyHistoryForVm(
            String nsId,
            String mciId,
            String vmId,
            String measurement,
            String startTime,
            String endTime) {
        return insightClient.getAnomalyHistoryForVm(
                nsId, mciId, vmId, measurement, startTime, endTime);
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
}
