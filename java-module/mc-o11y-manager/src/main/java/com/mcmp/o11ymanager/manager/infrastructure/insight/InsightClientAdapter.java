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
    public Object predictMetric(String nsId, String vmId, Object option) {
        return insightClient.predictMetric(nsId, vmId, option);
    }

    @Override
    public Object getPredictionHistory(
            String nsId, String vmId, String measurement, String startTime, String endTime) {
        return insightClient.getPredictionHistory(nsId, vmId, measurement, startTime, endTime);
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
    public Object getAnomalyDetection(String nsId, String vmId) {
        return insightClient.getAnomalyDetection(nsId, vmId);
    }

    @Override
    public Object getAnomalyDetectionHistory(
            String nsId, String vmId, String measurement, String startTime, String endTime) {
        return insightClient.getAnomalyDetectionHistory(
                nsId, vmId, measurement, startTime, endTime);
    }
}
