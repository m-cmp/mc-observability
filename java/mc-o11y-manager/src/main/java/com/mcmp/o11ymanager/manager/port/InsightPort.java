package com.mcmp.o11ymanager.manager.port;

public interface InsightPort {

    /* ===================== Prediction ===================== */
    Object getPredictionMeasurements();

    Object getPredictionSpecificMeasurement(String measurement);

    Object getPredictionOptions();

    Object predictMonitoringDataForMci(String nsId, String mciId, Object body);

    Object predictMonitoringDataForVm(String nsId, String mciId, String vmId, Object body);

    Object getPredictionHistoryForMci(
            String nsId, String mciId, String measurement, String startTime, String endTime);

    Object getPredictionHistoryForVm(
            String nsId,
            String mciId,
            String vmId,
            String measurement,
            String startTime,
            String endTime);

    /* ===================== Anomaly Detection ===================== */
    Object getMeasurements();

    Object getSpecificMeasurement(String measurement);

    Object getOptions();

    Object predictAnomaly(int settingSeq);

    Object getAnomalySettings();

    Object createAnomalySetting(Object body);

    Object updateAnomalySetting(int settingSeq, Object body);

    Object deleteAnomalySetting(int settingSeq);

    Object getAnomalySettingsForMci(String nsId, String mciId);

    Object getAnomalySettingsForVm(String nsId, String mciId, String vmId);

    Object getAnomalyHistoryForMci(
            String nsId, String mciId, String measurement, String startTime, String endTime);

    Object getAnomalyHistoryForVm(
            String nsId,
            String mciId,
            String vmId,
            String measurement,
            String startTime,
            String endTime);

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
