package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.AnomalyMetricType;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.TargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PredictionBody {

    private TargetType targetType;
    private AnomalyMetricType measurement;
}
