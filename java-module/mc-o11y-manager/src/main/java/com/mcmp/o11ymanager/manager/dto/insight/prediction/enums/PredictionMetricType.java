package com.mcmp.o11ymanager.manager.dto.insight.prediction.enums;

public enum PredictionMetricType {
    CPU("cpu"),
    MEM("mem"),
    DISK("disk"),
    SYSTEM("system");

    private final String value;

    PredictionMetricType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
