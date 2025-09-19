package com.mcmp.o11ymanager.manager.dto.insight.prediction;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PredictionOptions {

    private List<String> targetTypes;
    private List<String> measurements;
    private Map<String, String> predictionRanges;
}
