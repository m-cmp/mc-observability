package com.mcmp.o11ymanager.manager.enums;

import lombok.Getter;

@Getter
public enum Agent {
    TELEGRAF("Telegraf"),
    FLUENT_BIT("Fluent-Bit"),
    BEYLA("Beyla"),
    OTEL_JAVA_AGENT("Otel-Java-Agent");

    private final String name;

    Agent(String name) {
        this.name = name;
    }
}
