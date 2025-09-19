package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyDetectionHistoryValue {

    private String timestamp;
    private Double anomalyScore;
    private Integer isAnomaly;
    private Double value;
}
