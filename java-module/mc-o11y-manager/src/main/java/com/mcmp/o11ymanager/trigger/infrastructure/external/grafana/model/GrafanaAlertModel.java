package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Grafana alert model representing query data structure for alert rules Contains query
 * configuration, data source settings, and expression details used within GrafanaAlertQuery for
 * alert rule evaluation.
 */
@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaAlertModel {
    private GrafanaDataSource datasource;
    private Integer intervalMs;
    private Integer maxDataPoints;
    private String query;
    private boolean rawQuery;
    private List<GrafanaCondition> conditions;
    private String expression;
    private String reducer;
    private String refId;
    private String resultFormat;
    private Settings settings;
    private String type;
    private String queryType;
    private String expr;
    private String editorMode;

    @Builder
    public GrafanaAlertModel(
            GrafanaDataSource datasource,
            Integer intervalMs,
            Integer maxDataPoints,
            String query,
            boolean rawQuery,
            List<GrafanaCondition> conditions,
            String expression,
            String reducer,
            String refId,
            String resultFormat,
            Settings settings,
            String type,
            String queryType,
            String expr,
            String editorMode) {
        this.datasource = datasource;
        this.intervalMs = intervalMs;
        this.maxDataPoints = maxDataPoints;
        this.query = query;
        this.rawQuery = rawQuery;
        this.conditions = conditions;
        this.expression = expression;
        this.reducer = reducer;
        this.refId = refId;
        this.resultFormat = resultFormat;
        this.settings = settings;
        this.type = type;
        this.queryType = queryType;
        this.expr = expr;
        this.editorMode = editorMode;
    }

    /**
     * Settings nested class for query mode configuration Contains mode settings for query execution
     * behavior.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Settings {
        private String mode;
    }
}
