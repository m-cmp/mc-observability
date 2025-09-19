package com.mcmp.o11ymanager.manager.dto.insight.llm_analysis;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryMetadata {
    private List<String> queriesExecuted;
    private double totalExecutionTime;
    private int toolCallsCount;
    private List<String> databasesAccessed;
}
