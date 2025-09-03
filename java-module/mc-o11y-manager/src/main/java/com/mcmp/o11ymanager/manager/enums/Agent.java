package com.mcmp.o11ymanager.manager.enums;

import lombok.Getter;

@Getter
public enum Agent {
    TELEGRAF("Telegraf"),
    FLUENT_BIT("Fluent-Bit");

    private final String name;

    Agent(String name) {
        this.name = name;
    }
}
