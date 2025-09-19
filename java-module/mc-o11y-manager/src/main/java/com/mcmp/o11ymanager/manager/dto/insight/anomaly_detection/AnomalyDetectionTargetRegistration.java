package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.AnomalyMetricType;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.ExecutionInterval;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.TargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyDetectionTargetRegistration {

    private String nsId;
    private String targetId;
    private TargetType targetType;
    private AnomalyMetricType measurement;
    private ExecutionInterval executionInterval;

    public String getNsId() {
        return nsId;
    }

    public void setNsId(String nsId) {
        this.nsId = nsId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public AnomalyMetricType getMeasurement() {
        return measurement;
    }

    public void setMeasurement(AnomalyMetricType measurement) {
        this.measurement = measurement;
    }

    public ExecutionInterval getExecutionInterval() {
        return executionInterval;
    }

    public void setExecutionInterval(ExecutionInterval executionInterval) {
        this.executionInterval = executionInterval;
    }
}
