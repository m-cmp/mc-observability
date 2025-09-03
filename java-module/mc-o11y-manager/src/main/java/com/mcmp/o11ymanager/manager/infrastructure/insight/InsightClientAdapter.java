package com.mcmp.o11ymanager.manager.infrastructure.insight;

import com.mcmp.o11ymanager.manager.port.InsightPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InsightClientAdapter implements InsightPort {

    private final InsightClient insightClient;

    @Override
    public Object getPredictionMeasurement() {
        return insightClient.getPredictionMeasurement();
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
    public Object predictMetric(String nsId, String targetId, Object option) {
        return insightClient.predictMetric(nsId, targetId, option);
    }

    @Override
    public Object getPredictionHistory(
            String nsId, String targetId, String measurement, String startTime, String endTime) {
        return insightClient.getPredictionHistory(nsId, targetId, measurement, startTime, endTime);
    }

    @Override
    public Object getAnomalyDetectionMeasurement() {
        return insightClient.getAnomalyDetectionMeasurement();
    }

    @Override
    public Object getAnomalyDetectionSpecificMeasurement(String measurement) {
        return insightClient.getAnomalyDetectionSpecificMeasurement(measurement);
    }

    @Override
    public Object getAnomalyDetectionOptions() {
        return insightClient.getAnomalyDetectionOptions();
    }

    @Override
    public Object getAnomalyDetectionSettings() {
        return insightClient.getAnomalyDetectionSettings();
    }

    @Override
    public Object insertAnomalyDetectionSetting(Object body) {
        return insightClient.insertAnomalyDetectionSetting(body);
    }

    @Override
    public Object updateAnomalyDetectionSetting(Long settingSeq, Object body) {
        return insightClient.updateAnomalyDetectionSetting(settingSeq, body);
    }

    @Override
    public Object deleteAnomalyDetectionSetting(Long settingSeq) {
        return insightClient.deleteAnomalyDetectionSetting(settingSeq);
    }

    @Override
    public Object getAnomalyDetection(String nsId, String targetId) {
        return insightClient.getAnomalyDetection(nsId, targetId);
    }

    @Override
    public Object getAnomalyDetectionHistory(
            String nsId, String targetId, String measurement, String startTime, String endTime) {
        return insightClient.getAnomalyDetectionHistory(
                nsId, targetId, measurement, startTime, endTime);
    }
}
