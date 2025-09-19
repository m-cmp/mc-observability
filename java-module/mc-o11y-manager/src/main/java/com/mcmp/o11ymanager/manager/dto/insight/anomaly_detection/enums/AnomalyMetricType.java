package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums;

public enum AnomalyMetricType {
    CPU("cpu"),
    MEM("mem");

    private final String value;

    AnomalyMetricType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
