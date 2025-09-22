package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyDetectionHistoryResponse {
    private String nsId;
    private String targetId;
    private String measurement;
    private List<AnomalyDetectionHistoryValue> values;
}
