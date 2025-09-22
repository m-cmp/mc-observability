package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.*;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.*;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.*;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import java.util.List;

public interface InsightPort {

    /* ===================== Prediction ===================== */
    ResBody<List<PredictionMeasurement>> getPredictionMeasurements();

    ResBody<PredictionMeasurement> getPredictionSpecificMeasurement(String measurement);

    ResBody<PredictionOptions> getPredictionOptions();

    ResBody<PredictionResult> predictMonitoringData(String nsId, String vmId, PredictionBody body);

    ResBody<PredictionHistory> getPredictionHistory(
            String nsId, String vmId, String measurement, String startTime, String endTime);

    /* ===================== Anomaly Detection ===================== */
    ResBody<List<AnomalyDetectionMeasurement>> getMeasurements();

    ResBody<AnomalyDetectionMeasurement> getSpecificMeasurement(String measurement);

    ResBody<AnomalyDetectionOptions> getOptions();

    ResBody<PredictionResult> predictMetric(String nsId, String targetId, PredictionBody body);

    ResBody<PredictionHistory> getAnomalyHistory(
            String nsId, String targetId, String measurement, String startTime, String endTime);

    /* ===================== LLM ===================== */
    ResBody<List<LLMModel>> getLLMModelOptions();

    ResBody<List<LLMChatSession>> getLLMChatSessions();

    ResBody<LLMChatSession> postLLMChatSession(PostSessionBody body);

    ResBody<LLMChatSession> deleteLLMChatSession(String sessionId);

    ResBody<List<LLMChatSession>> deleteAllLLMChatSessions();

    ResBody<SessionHistory> getLLMSessionHistory(String sessionId);

    /* ===================== Alert Analysis ===================== */
    ResBody<Message> queryAlertAnalysis(PostQueryBody body);

    /* ===================== Log Analysis ===================== */
    ResBody<Message> queryLogAnalysis(PostQueryBody body);
}
