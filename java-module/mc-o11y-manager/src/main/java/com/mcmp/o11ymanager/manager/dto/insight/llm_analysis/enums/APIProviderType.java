package com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.enums;

public enum APIProviderType {
    OPENAI("openai"),
    GOOGLE("google"),
    ANTHROPIC("anthropic");

    private final String value;

    APIProviderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
