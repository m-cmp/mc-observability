package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyDetectionMeasurement {

    private int pluginSeq;
    private String measurement;
    private List<Map<String, String>> fields;

    public int getPluginSeq() {
        return pluginSeq;
    }

    public void setPluginSeq(int pluginSeq) {
        this.pluginSeq = pluginSeq;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public List<Map<String, String>> getFields() {
        return fields;
    }

    public void setFields(List<Map<String, String>> fields) {
        this.fields = fields;
    }
}
