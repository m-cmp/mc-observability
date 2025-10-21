package com.mcmp.o11ymanager.trigger.application.common.type;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum ResourceType {
    CPU("cpu", "usage_idle"),
    MEMORY("mem", "used_percent"),
    DISK("disk", "used_percent");

    private final String measurement;
    private final String field;

    ResourceType(String measurement, String field) {
        this.measurement = measurement;
        this.field = field;
    }

    public static ResourceType findBy(String measurement) {
        return Arrays.stream(ResourceType.values())
                .filter(type -> type.measurement.equals(measurement))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown resource type"));
    }
}
