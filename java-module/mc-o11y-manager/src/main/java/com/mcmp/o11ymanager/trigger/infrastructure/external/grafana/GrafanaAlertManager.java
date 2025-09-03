package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmp.o11ymanager.trigger.adapter.external.alert.AlertManager;
import com.mcmp.o11ymanager.trigger.adapter.external.alert.dto.AlertRuleCreateDto;
import com.mcmp.o11ymanager.trigger.adapter.internal.trigger.ManagerPort;
import com.mcmp.o11ymanager.trigger.application.common.exception.GrafanaAlertTestFailException;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.GrafanaHealthCheckResponse;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.GrafanaHealthCheckResponse.HealthStatus;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.GrafanaAlertRule;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.contact.AlertConfig;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.contact.AlertConfig.GrafanaManagedReceiverConfig;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.query.QueryFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Main service class for managing Grafana alert operations and health monitoring Implements
 * AlertManager interface to provide alert rule management, health checks, and integration with
 * Grafana's alerting and provisioning APIs.
 */
@Slf4j
@Component
public class GrafanaAlertManager implements AlertManager {

    private final GrafanaClient grafanaClient;
    private final GrafanaAlertRuleFactory alertRuleFactory;
    private final ObjectMapper objectMapper;

    @Value("${grafana.alert.orgId}")
    private Long orgId;

    @Value("${grafana.alert.folder.uid}")
    private String folderUid;

    @Value("${grafana.alert.folder.name}")
    private String folderName;

    @Value("${grafana.alert.receiver}")
    private String receiver;

    @Value("${influxdb.servers[0].uid}")
    private String datasourceUid1;

    @Value("${influxdb.servers[1].uid}")
    private String datasourceUid2;

    /**
     * Constructor for GrafanaAlertManager.
     *
     * @param grafanaClient HTTP client for Grafana API interactions
     * @param alertRuleFactory factory for creating Grafana alert rule configurations
     */
    public GrafanaAlertManager(
            GrafanaClient grafanaClient,
            GrafanaAlertRuleFactory alertRuleFactory,
            ObjectMapper objectMapper,
            ManagerPort managerPort) {
        this.grafanaClient = grafanaClient;
        this.alertRuleFactory = alertRuleFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new alert rule in Grafana with validation testing. Generates FluxQL query, creates
     * alert rule configuration, posts to Grafana, tests the rule, and rolls back if testing fails.
     *
     * @param dto alert rule creation data transfer object
     * @throws RuntimeException if rule creation or testing fails
     */
    @Override
    public void createAlertRule(AlertRuleCreateDto dto, String datasourceUid) {
        String query = QueryFactory.fluxQL(dto);

        GrafanaAlertRule alertRule =
                alertRuleFactory.createGrafanaAlertRule(
                        dto.uuid(),
                        dto.namespaceId() + "-" + dto.targetScope() + "-" + dto.targetId(),
                        dto.holdDuration(),
                        query,
                        dto.thresholdExpression(),
                        dto.repeatInterval(),
                        dto.targetScope(),
                        dto.title(),
                        datasourceUid);

        GrafanaClientWrapper.call(
                () -> {
                    ResponseEntity<Object> result = grafanaClient.createAlertRule(alertRule);
                    Map<String, Object> createdAlertRule =
                            Map.of("grafana_alert", result.getBody());
                    Map<String, Object> testObject =
                            Map.of(
                                    "rule",
                                    createdAlertRule,
                                    "folderTitle",
                                    folderName,
                                    "folderUid",
                                    folderUid,
                                    "ruleGroup",
                                    dto.title());

                    try {
                        grafanaClient.testAlertRule(testObject);
                    } catch (Exception e) {
                        if (createdAlertRule.get("grafana_alert")
                                instanceof Map<?, ?> grafanaAlert) {
                            grafanaClient.deleteAlertRule(grafanaAlert.get("uid").toString());
                        }
                        throw new GrafanaAlertTestFailException(
                                "Failed to test grafana alert rule: " + dto.title(), e);
                    }
                });
    }

    /**
     * Deletes an existing alert rule from Grafana by UUID.
     *
     * @param uuid unique identifier of the alert rule to delete
     * @throws RuntimeException if deletion fails
     */
    @Override
    public void deleteAlertRule(String uuid) {
        GrafanaClientWrapper.call(() -> grafanaClient.deleteAlertRule(uuid));
    }

    /**
     * Retrieves all active alerts from Grafana Alertmanager.
     *
     * @return response object containing all alerts data
     * @throws RuntimeException if retrieval fails
     */
    public Object getAllAlerts() {
        return GrafanaClientWrapper.call(() -> grafanaClient.getAllAlerts().getBody());
    }

    /**
     * Retrieves alerts filtered by rule name/title.
     *
     * @param title alert rule name to filter by
     * @return response object containing filtered alerts data
     * @throws RuntimeException if retrieval fails
     */
    public Object getAlertBy(String title) {
        return GrafanaClientWrapper.call(
                () -> grafanaClient.getAlertBy("rulename=" + title).getBody());
    }

    /**
     * Retrieves all configured alert rules from Grafana.
     *
     * @return response object containing all alert rules data
     * @throws RuntimeException if retrieval fails
     */
    public Object getAllAlertRules() {
        return GrafanaClientWrapper.call(() -> grafanaClient.getAllAlertRules().getBody());
    }

    /**
     * Retrieves all configured contact points for alert notifications.
     *
     * @return list of Grafana-managed receiver configurations
     * @throws RuntimeException if retrieval fails
     */
    public List<GrafanaManagedReceiverConfig> getAllContactPoints() {
        return GrafanaClientWrapper.call(() -> grafanaClient.getAllContactPoints().getBody());
    }

    public Object getDatasourceHealthBy(String uid) {
        return GrafanaClientWrapper.call(() -> grafanaClient.getDatasourceHealthBy(uid).getBody());
    }

    public Object getFolder(String folderUid) {
        return GrafanaClientWrapper.call(() -> grafanaClient.getFolderBy(folderUid).getBody());
    }

    public Object getOrgBy(Long orgId) {
        return GrafanaClientWrapper.call(() -> grafanaClient.getOrgBy(orgId).getBody());
    }

    /**
     * Tests alert receiver configuration by sending test notifications. Retrieves all contact
     * points and creates test configuration to verify notification delivery.
     *
     * @throws RuntimeException if test fails
     */
    public void testAlertReceiver() {
        GrafanaClientWrapper.call(
                () -> {
                    ResponseEntity<List<GrafanaManagedReceiverConfig>> allAlertReceivers =
                            grafanaClient.getAllContactPoints();
                    AlertConfig AlertTestConfig =
                            AlertConfig.createAlertTestConfig(allAlertReceivers.getBody());
                    grafanaClient.testAlertReceiver(AlertTestConfig);
                });
    }

    /**
     * Performs comprehensive health check of Grafana configuration and dependencies. Checks contact
     * points, data sources, folders, rule groups, and organization settings.
     *
     * @return GrafanaHealthCheckResponse containing health status of all components
     */
    public GrafanaHealthCheckResponse checkGrafanaHealth() {
        return GrafanaHealthCheckResponse.builder()
                .contactPoint(checkContactPoint())
                .datasource(checkDatasource())
                .folder(checkFolder())
                .org(checkOrg())
                .build();
    }

    public List<Map<String, Object>> getAllDatasources() {
        return GrafanaClientWrapper.call(() -> grafanaClient.getAllDatasources().getBody());
    }

    /**
     * Checks if the configured contact point/receiver exists in Grafana.
     *
     * @return HealthStatus indicating whether the receiver is available
     */
    private HealthStatus checkContactPoint() {
        List<GrafanaManagedReceiverConfig> allContactPoints = getAllContactPoints();
        return healthStatus(
                () ->
                        allContactPoints.stream()
                                .anyMatch(config -> config.getName().equals(receiver)));
    }

    private HealthStatus checkDatasource() {
        return healthStatus(
                () -> {
                    Map<String, String> result1 =
                            (HashMap<String, String>) getDatasourceHealthBy(datasourceUid1);
                    Map<String, String> result2 =
                            (HashMap<String, String>) getDatasourceHealthBy(datasourceUid2);
                    return result1.get("status").equals("OK") && result2.get("status").equals("OK");
                });
    }

    private HealthStatus checkFolder() {
        return healthStatus(() -> getFolder(folderUid));
    }

    private HealthStatus checkOrg() {
        return healthStatus(() -> getOrgBy(orgId));
    }

    /**
     * Executes a health check operation that returns a boolean result.
     *
     * @param supplier function that performs the health check and returns boolean
     * @return HealthStatus with the check result, false if exception occurs
     */
    private HealthStatus healthStatus(Supplier<Boolean> supplier) {
        try {
            return new HealthStatus(supplier.get());
        } catch (Exception e) {
            return new HealthStatus(false);
        }
    }

    /**
     * Executes a health check operation that performs an action without return value.
     *
     * @param runnable function that performs the health check action
     * @return HealthStatus with true if successful, false if exception occurs
     */
    private HealthStatus healthStatus(Runnable runnable) {
        try {
            runnable.run();
            return new HealthStatus(true);
        } catch (Exception e) {
            return new HealthStatus(false);
        }
    }
}
