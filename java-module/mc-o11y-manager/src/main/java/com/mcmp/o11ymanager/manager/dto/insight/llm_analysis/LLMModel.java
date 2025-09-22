package com.mcmp.o11ymanager.manager.dto.insight.llm_analysis;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LLMModel {
    private String provider; // ollama, openai, google, anthropic
    private List<String> modelName;
}
