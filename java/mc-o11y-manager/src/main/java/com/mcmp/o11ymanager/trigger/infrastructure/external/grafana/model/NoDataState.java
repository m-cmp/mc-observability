package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import lombok.RequiredArgsConstructor;

/**
 * Enumeration for alert rule no-data states Defines behavior when no data is available for alert
 * evaluation.
 */
@RequiredArgsConstructor
public enum NoDataState {
    OK("OK"),
    ALERTING("Alerting"),
    NO_DATA("NoData");

    private final String value;
}
