package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.GrafanaCondition.*;

import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.*;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.GrafanaCondition.Evaluator;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.GrafanaCondition.Operator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory class for creating Grafana alert rule configurations Generates GrafanaAlertRule objects
 * that conform to the Grafana ProvisionedAlertRule schema. Creates complex alert queries with data
 * source integration, expression evaluation, and threshold conditions.
 */
@Component
public class GrafanaAlertRuleFactory {

    @Value("${grafana.alert.orgId}")
    private Integer orgId;

    @Value("${grafana.alert.folder.uid}")
    private String folderUid;

    @Value("${grafana.alert.receiver}")
    private String receiver;

    @Value("${grafana.alert.noDataState}")
    private String noDataState;

    @Value("${grafana.alert.execErrState}")
    private String execErrState;

    private final Map<String, String> metricLabels =
            new HashMap<>(1) {
                {
                    put("level", "{{ $values.C.Value }}");
                }
            };
    private final Map<String, String> metricAnnotations = Map.of("alertType", "metric");

    /**
     * Creates a complete Grafana alert rule with metric monitoring configuration. Builds a
     * ProvisionedAlertRule compliant object with data queries, threshold evaluation, and
     * notification settings.
     *
     * @param uid unique identifier for the alert rule
     * @param title display name of the alert rule
     * @param duration how long the condition must be true before alerting
     * @param query FluxQL query for data retrieval
     * @param thresholdCondition threshold expression defining info/warning/critical levels
     * @param repeatInterval interval for re-sending the alert when the same condition/state
     *     continues.
     * @param vmScope scope identifier for the monitoring vm
     * @return GrafanaAlertRule configured for the specified parameters
     */
    public GrafanaAlertRule createGrafanaAlertRule(
            String uid,
            String title,
            String duration,
            String query,
            String thresholdCondition,
            String repeatInterval,
            String vmScope,
            String ruleGroup,
            String datasourceUid,
        String resourceType) {
        List<GrafanaAlertQuery> alertData =
                createMetricAlertData(query, thresholdCondition, datasourceUid);
        metricLabels.put("vmScope", vmScope);
        metricLabels.put("ruleGroup", ruleGroup);
        metricLabels.put("resourceType", resourceType);

        return GrafanaAlertRule.builder()
                .uid(uid)
                .annotations(metricAnnotations)
                .orgId(orgId)
                .folderUid(folderUid)
                .ruleGroup(ruleGroup)
                .title(title)
                .condition("C")
                .data(alertData)
                .noDataState(noDataState)
                .execErrState(execErrState)
                .duration(duration)
                .labels(metricLabels)
                .isPaused(true)
                .notificationSettings(
                        GrafanaNotificationSetting.builder()
                                .receiver(receiver)
                                .groupBy(List.of("grafana_folder", "ruleGroup"))
                                .groupWait("0s")
                                .groupInterval("1s")
                                .repeatInterval(repeatInterval)
                                .build())
                .build();
    }

    /**
     * Creates the data query structure for metric-based alert rules. Builds a three-query pipeline:
     * A) data retrieval, B) reduction, C) threshold evaluation. This follows Grafana's expression
     * query pattern for alert evaluation.
     *
     * @param query FluxQL query for retrieving metric data
     * @param thresholdCondition threshold expression for alert level determination
     * @return list of GrafanaAlertQuery objects representing the evaluation pipeline
     */
    private List<GrafanaAlertQuery> createMetricAlertData(
            String query, String thresholdCondition, String datasourceUid) {
        GrafanaAlertQuery dataA =
                GrafanaAlertQuery.builder()
                        .refId("A")
                        .queryType("")
                        .relativeTimeRange(new TimeRange(600, 0))
                        .datasourceUid(datasourceUid)
                        .model(
                                GrafanaAlertModel.builder()
                                        .datasource(
                                                GrafanaDataSource.builder()
                                                        .type("influxdb")
                                                        .uid(datasourceUid)
                                                        .build())
                                        .query(query)
                                        .intervalMs(1000)
                                        .maxDataPoints(43200)
                                        .rawQuery(true)
                                        .refId("A")
                                        .resultFormat("time_series")
                                        .queryType("")
                                        .build())
                        .build();

        GrafanaAlertQuery dataB =
                GrafanaAlertQuery.builder()
                        .refId("B")
                        .queryType("")
                        .relativeTimeRange(new TimeRange(10, 0))
                        .datasourceUid("__expr__")
                        .model(
                                GrafanaAlertModel.builder()
                                        .conditions(
                                                List.of(
                                                        builder()
                                                                .type("query")
                                                                .evaluator(
                                                                        new Evaluator(
                                                                                List.of(0, 0),
                                                                                "gt"))
                                                                .operator(new Operator("and"))
                                                                .query(new Query(List.of()))
                                                                .reducer(
                                                                        new Reducer(
                                                                                List.of(), "avg"))
                                                                .build()))
                                        .datasource(
                                                GrafanaDataSource.builder()
                                                        .name("Expression")
                                                        .type("expr")
                                                        .uid("expr")
                                                        .build())
                                        .expression("A")
                                        .intervalMs(1000)
                                        .maxDataPoints(43200)
                                        .reducer("last")
                                        .refId("B")
                                        .settings(new GrafanaAlertModel.Settings("dropNN"))
                                        .type("reduce")
                                        .queryType("")
                                        .build())
                        .build();

        GrafanaAlertQuery dataC =
                GrafanaAlertQuery.builder()
                        .refId("C")
                        .queryType("")
                        .relativeTimeRange(new TimeRange(0, 0))
                        .datasourceUid("__expr__")
                        .model(
                                GrafanaAlertModel.builder()
                                        .conditions(
                                                List.of(
                                                        builder()
                                                                .type("query")
                                                                .evaluator(
                                                                        new Evaluator(
                                                                                List.of(0, 0),
                                                                                "gt"))
                                                                .operator(new Operator("and"))
                                                                .query(new Query(List.of()))
                                                                .reducer(
                                                                        new Reducer(
                                                                                List.of(), "avg"))
                                                                .build()))
                                        .datasource(
                                                GrafanaDataSource.builder()
                                                        .name("Expression")
                                                        .type("expr")
                                                        .uid("expr")
                                                        .build())
                                        .expression(grafanaExpression(thresholdCondition))
                                        .intervalMs(1000)
                                        .maxDataPoints(43200)
                                        .refId("C")
                                        .type("math")
                                        .queryType("")
                                        .build())
                        .build();

        return List.of(dataA, dataB, dataC);
    }

    /**
     * Converts threshold condition into Grafana expression syntax. Creates a mathematical
     * expression that evaluates metric values against thresholds and returns alert level codes
     * (1=INFO, 2=WARNING, 3=CRITICAL).
     *
     * @param thresholdExpression string representation of threshold levels
     * @return Grafana expression for threshold evaluation
     */
    private String grafanaExpression(String thresholdExpression) {
        ThresholdCondition condition = ThresholdCondition.from(thresholdExpression);
        String sb =
                "($B > "
                        + condition.critical()
                        + ") * 1"
                        + " + "
                        + "($B > "
                        + condition.warning()
                        + ") * 1"
                        + " + "
                        + "($B > "
                        + condition.info()
                        + ") * 1";
        return sb;
    }
}
