package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.enums.ExecutionInterval;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyDetectionTargetUpdate {

    private ExecutionInterval executionInterval;

    public ExecutionInterval getExecutionInterval() {
        return executionInterval;
    }

    public void setExecutionInterval(ExecutionInterval executionInterval) {
        this.executionInterval = executionInterval;
    }
}
