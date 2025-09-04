package com.mcmp.o11ymanager.manager.port;

public interface InsightPort {
    Object getPredictionMeasurement();

    Object getPredictionSpecificMeasurement(String measurement);

    Object getPredictionOptions();

    Object predictMetric(String nsId, String targetId, Object option);

    Object getPredictionHistory(
            String nsId, String targetId, String measurement, String startTime, String endTime);

    Object getAnomalyDetectionMeasurement();

    Object getAnomalyDetectionSpecificMeasurement(String measurement);

    Object getAnomalyDetectionOptions();

    Object getAnomalyDetectionSettings();

    Object insertAnomalyDetectionSetting(Object body);

    Object updateAnomalyDetectionSetting(Long settingSeq, Object body);

    Object deleteAnomalyDetectionSetting(Long settingSeq);

    Object getAnomalyDetection(String nsId, String targetId);

    Object getAnomalyDetectionHistory(
            String nsId, String targetId, String measurement, String startTime, String endTime);
}
