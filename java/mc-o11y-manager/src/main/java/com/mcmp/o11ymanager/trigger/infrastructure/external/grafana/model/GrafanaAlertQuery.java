package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Grafana alert query model representing individual query configuration within alert rules Contains
 * query reference ID, data source configuration, and query model details.
 */
@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaAlertQuery {
    private String refId;
    private String queryType;
    private TimeRange relativeTimeRange;
    private String datasourceUid;
    private GrafanaAlertModel model;

    @Builder
    public GrafanaAlertQuery(
            String refId,
            String queryType,
            TimeRange relativeTimeRange,
            String datasourceUid,
            GrafanaAlertModel model) {
        this.refId = refId;
        this.queryType = queryType;
        this.relativeTimeRange = relativeTimeRange;
        this.datasourceUid = datasourceUid;
        this.model = model;
    }
}
