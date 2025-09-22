package com.mcmp.o11ymanager.manager.dto.insight.llm_analysis;

import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.enums.ProviderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostSessionBody {
    private ProviderType provider;
    private String modelName;
}
