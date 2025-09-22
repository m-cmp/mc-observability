package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums;

public enum ExecutionInterval {
    M5("5m"),
    M10("10m"),
    M30("30m");

    private final String value;

    ExecutionInterval(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
