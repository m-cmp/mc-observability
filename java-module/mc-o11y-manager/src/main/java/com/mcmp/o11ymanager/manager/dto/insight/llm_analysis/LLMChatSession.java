package com.mcmp.o11ymanager.manager.dto.insight.llm_analysis;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LLMChatSession {

    private int seq;
    private String userId;
    private String sessionId;
    private String provider;
    private String modelName;
    private LocalDateTime regdate;
}
