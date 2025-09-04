package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.message;

import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * Grafana alert message model representing webhook payload from Grafana Contains grouped alerts,
 * common metadata, and notification receiver information. Root object received when Grafana sends
 * alert notifications via webhook.
 */
@Getter
public class GrafanaAlertMessage {
    private String receiver;
    private String status;
    private List<GrafanaAlert> alerts;
    private Map<String, String> commonLabels;
    private Map<String, String> commonAnnotations;
    private String state;
    private Integer version;
    private String externalURL;
    private String groupKey;
    private String message;
}
