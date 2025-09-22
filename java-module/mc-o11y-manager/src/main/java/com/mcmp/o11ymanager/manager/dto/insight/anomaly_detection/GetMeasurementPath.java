package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetMeasurementPath {
    private String measurement;

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }
}
