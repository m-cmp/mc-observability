package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.message;

import java.util.Map;
import lombok.Getter;

/**
 * Grafana alert model representing individual alert instance within webhook messages Contains alert
 * status, metadata, timing information, and associated URLs. Part of webhook payload received from
 * Grafana when alerts are triggered.
 */
@Getter
public class GrafanaAlert {
    private String status;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private String startsAt;
    private String endsAt;
    private String generatorURL;
    private String fingerprint;
    private String silenceURL;
    private String dashboardURL;
    private String panelURL;
    private Map<String, Double> values;
    private String valueString;
}
