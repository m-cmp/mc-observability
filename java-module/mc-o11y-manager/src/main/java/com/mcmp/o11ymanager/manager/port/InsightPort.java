package com.mcmp.o11ymanager.manager.port;

public interface InsightPort {

    /* ===================== Prediction ===================== */
    Object getPredictionMeasurements();

    Object getPredictionSpecificMeasurement(String measurement);

    Object getPredictionOptions();

    Object predictMonitoringData(String nsId, String vmId, Object body);

    Object getPredictionHistory(
            String nsId, String vmId, String measurement, String startTime, String endTime);

    /* ===================== Anomaly Detection ===================== */
    Object getMeasurements();

    Object getSpecificMeasurement(String measurement);

    Object getOptions();

    Object predictAnomaly(int settingSeq, Object body);

    Object getAnomalySettings();

    Object createAnomalySetting(Object body);

    Object updateAnomalySetting(int settingSeq, Object body);

    Object deleteAnomalySetting(int settingSeq);

    Object getAnomalyHistory(
            String nsId, String targetId, String measurement, String startTime, String endTime);

    /* ===================== LLM ===================== */
    Object getLLMModelOptions();

    Object getLLMChatSessions();

    Object postLLMChatSession(Object body);

    Object deleteLLMChatSession(String sessionId);

    Object deleteAllLLMChatSessions();

    Object getLLMSessionHistory(String sessionId);

    Object getLLMApiKeys(String provider);

    Object postLLMApiKeys(Object body);

    Object deleteLLMApiKeys(String provider);

    /* ===================== Alert Analysis ===================== */
    Object queryAlertAnalysis(Object body);

    /* ===================== Log Analysis ===================== */
    Object queryLogAnalysis(Object body);
}
