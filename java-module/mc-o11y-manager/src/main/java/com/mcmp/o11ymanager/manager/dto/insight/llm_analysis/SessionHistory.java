package com.mcmp.o11ymanager.manager.dto.insight.llm_analysis;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionHistory {

    private List<Message> messages;
    private int seq;
    private String userId;
    private String sessionId;
    private String provider;
    private String modelName;
    private LocalDateTime regdate;
}
