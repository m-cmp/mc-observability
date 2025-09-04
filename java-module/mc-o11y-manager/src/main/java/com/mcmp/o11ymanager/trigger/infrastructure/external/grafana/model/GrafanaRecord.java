package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

/**
 * Grafana record model for recording alert rule metadata Contains source information and metric
 * details for alert rule recording.
 */
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaRecord {
    @NotNull private String from;
    @NotNull private String metric;
}
