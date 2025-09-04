package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Time range model for query time window configuration Defines relative time range boundaries for
 * alert query evaluation. Used within GrafanaAlertQuery to specify query time scope.
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TimeRange {
    private Integer from;
    private Integer to;
}
