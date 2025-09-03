package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import lombok.RequiredArgsConstructor;

/**
 * Enumeration for alert rule execution error states Defines behavior when an alert rule encounters
 * execution errors.
 */
@RequiredArgsConstructor
public enum ExecErrState {
    OK("OK"),
    ALERTING("Alerting"),
    ERROR("Error");

    private final String value;
}
