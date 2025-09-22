package com.mcmp.o11ymanager.manager.dto.insight.llm_analysis;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Message {

    private String messageType;
    private String message;
    private QueryMetadata metadata;
}
