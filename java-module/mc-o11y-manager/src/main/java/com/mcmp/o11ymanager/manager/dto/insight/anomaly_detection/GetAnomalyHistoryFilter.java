package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.AnomalyMetricType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAnomalyHistoryFilter {

    private AnomalyMetricType measurement;
    private String startTime;
    private String endTime;

    public AnomalyMetricType getMeasurement() {
        return measurement;
    }

    public void setMeasurement(AnomalyMetricType measurement) {
        this.measurement = measurement;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
