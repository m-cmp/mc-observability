package com.mcmp.o11ymanager.manager.port;

public interface InsightPort {

    /* ===================== Prediction ===================== */
    Object getPredictionMeasurements();

    Object getPredictionSpecificMeasurement(String measurement);

    Object getPredictionOptions();

    Object predictMonitoringDataForInfra(String nsId, String infraId, Object body);

    Object predictMonitoringDataForNode(String nsId, String infraId, String nodeId, Object body);

    Object getPredictionHistoryForInfra(
            String nsId, String infraId, String measurement, String startTime, String endTime);

    Object getPredictionHistoryForNode(
            String nsId,
            String infraId,
            String nodeId,
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

    Object getAnomalySettingsForInfra(String nsId, String infraId);

    Object getAnomalySettingsForNode(String nsId, String infraId, String nodeId);

    Object getAnomalyHistoryForInfra(
            String nsId, String infraId, String measurement, String startTime, String endTime);

    Object getAnomalyHistoryForNode(
            String nsId,
            String infraId,
            String nodeId,
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

    /* ===================== Server Error Analysis ===================== */
    Object detectServerError(Object body);

    Object queryServerError(Object body);

    Object listServerErrorRecords(
            String status, String fromDt, String toDt, Integer page, Integer size);

    Object getServerErrorRecord(int analysisId);

    Object rerunServerErrorAnalysis(int analysisId);
}
