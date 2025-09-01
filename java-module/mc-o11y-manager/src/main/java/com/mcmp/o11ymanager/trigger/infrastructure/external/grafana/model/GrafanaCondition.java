package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Grafana condition model for alert rule evaluation logic Defines conditions, evaluators,
 * operators, and reducers for alert rule processing. Used within GrafanaAlertModel to specify alert
 * triggering conditions.
 */
@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaCondition {
    private String type;
    private Evaluator evaluator;
    private Operator operator;
    private Query query;
    private Reducer reducer;

    @Builder
    public GrafanaCondition(
            String type, Evaluator evaluator, Operator operator, Query query, Reducer reducer) {
        this.type = type;
        this.evaluator = evaluator;
        this.operator = operator;
        this.query = query;
        this.reducer = reducer;
    }

    /**
     * Evaluator nested class for condition evaluation parameters Contains evaluation type and
     * parameters for threshold comparison.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Evaluator {
        private List<Integer> params;
        private String type;
    }

    /**
     * Operator nested class for logical operation configuration Defines the type of logical
     * operator used in condition evaluation.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Operator {
        private String type;
    }

    /**
     * Query nested class for query parameter configuration Contains parameters used in query-based
     * condition evaluation.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Query {
        private List<Integer> params;
    }

    /**
     * Reducer nested class for data reduction configuration Defines how multiple data points are
     * reduced to a single value for evaluation.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reducer {
        private List<Integer> params;
        private String type;
    }
}
