package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums;

public enum TargetType {
    VM("vm"),
    MCI("mci");

    private final String value;

    TargetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
