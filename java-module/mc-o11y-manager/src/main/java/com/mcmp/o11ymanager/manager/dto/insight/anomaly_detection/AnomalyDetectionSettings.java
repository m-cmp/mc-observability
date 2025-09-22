package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyDetectionSettings {
    private int seq;
    private String nsId;
    private String targetId;
    private String targetType;
    private String measurement;
    private String executionInterval;
    private String lastExecution;
    private String createAt;
}
