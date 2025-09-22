package com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.enums;

public enum ProviderType {
    OPENAI("openai"),
    OLLAMA("ollama"),
    GOOGLE("google"),
    ANTHROPIC("anthropic");

    private final String value;

    ProviderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
