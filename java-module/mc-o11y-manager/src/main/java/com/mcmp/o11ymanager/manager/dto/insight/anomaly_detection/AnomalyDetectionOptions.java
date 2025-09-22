package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyDetectionOptions {

    private List<String> targetTypes;
    private List<String> measurements;
    private List<String> executionIntervals;
}
