package com.mcmp.o11ymanager.manager.dto.insight.prediction;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PredictionMeasurement {

    private int pluginSeq;
    private String measurement;
    private List<Map<String, String>> fields;
}
