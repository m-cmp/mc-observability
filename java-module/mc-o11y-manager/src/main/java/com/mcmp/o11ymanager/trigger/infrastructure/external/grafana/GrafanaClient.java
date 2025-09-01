package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana;

import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.GrafanaAlertRule;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.contact.AlertConfig;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.contact.AlertConfig.GrafanaManagedReceiverConfig;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * HTTP client interface for interacting with Grafana API. Provides methods for managing alert
 * rules, contact points, and monitoring data sources. Uses Spring's HTTP Exchange annotations for
 * declarative HTTP client configuration.
 */
@HttpExchange
public interface GrafanaClient {

    /**
     * Creates a new alert rule in Grafana using the provisioning API.
     *
     * @param dto - the Grafana alert rule configuration
     * @return ResponseEntity containing the created alert rule response
     */
    @PostExchange("/api/v1/provisioning/alert-rules")
    ResponseEntity<Object> createAlertRule(@RequestBody GrafanaAlertRule dto);

    /**
     * Deletes an existing alert rule from Grafana by its unique identifier.
     *
     * @param uid - the unique identifier of the alert rule to delete
     * @return ResponseEntity with no content on successful deletion
     */
    @DeleteExchange("/api/v1/provisioning/alert-rules/{uid}")
    ResponseEntity<Void> deleteAlertRule(@PathVariable String uid);

    /**
     * Retrieves all alert rules configured in Grafana.
     *
     * @return ResponseEntity containing list of all alert rules
     */
    @GetExchange("/api/v1/provisioning/alert-rules")
    ResponseEntity<Object> getAllAlertRules();

    /**
     * Retrieves all alerts from Grafana Alertmanager.
     *
     * @return ResponseEntity containing list of all alerts
     */
    @GetExchange("/api/alertmanager/grafana/api/v2/alerts")
    ResponseEntity<Object> getAllAlerts();

    /**
     * Retrieves alerts from Grafana Alertmanager with optional filtering.
     *
     * @param filter - optional filter criteria for alerts
     * @return ResponseEntity containing filtered alerts
     */
    @GetExchange("/api/alertmanager/grafana/api/v2/alerts")
    ResponseEntity<Object> getAlertBy(@RequestParam(required = false) String filter);

    /**
     * Tests an alert rule configuration against Grafana's evaluation engine.
     *
     * @param dto - the alert rule test configuration
     * @return ResponseEntity containing test results
     */
    @PostExchange("/api/v1/rule/test/grafana")
    ResponseEntity<Object> testAlertRule(@RequestBody Map<String, Object> dto);

    /**
     * Retrieves all configured contact points for alert notifications.
     *
     * @return ResponseEntity containing list of contact point configurations
     */
    @GetExchange("/api/v1/provisioning/contact-points")
    ResponseEntity<List<GrafanaManagedReceiverConfig>> getAllContactPoints();

    /**
     * Tests an alert receiver configuration to verify notification delivery.
     *
     * @param dto - the alert receiver configuration to test
     * @return ResponseEntity containing test results
     */
    @PostExchange("/api/alertmanager/grafana/config/api/v1/receivers/test")
    ResponseEntity<Object> testAlertReceiver(@RequestBody AlertConfig dto);

    /**
     * Checks the health status of a specific data source by its unique identifier.
     *
     * @param uid - the unique identifier of the data source
     * @return ResponseEntity containing data source health information
     */
    @GetExchange("/api/datasources/uid/{uid}/health")
    ResponseEntity<Object> getDatasourceHealthBy(@PathVariable String uid);

  @GetExchange("/api/datasources")
  ResponseEntity<List<Map<String, Object>>> getAllDatasources();

    /**
     * Retrieves folder information by its unique identifier.
     *
     * @param folderUid - the unique identifier of the folder
     * @return ResponseEntity containing folder details
     */
    @GetExchange("/api/folders/{folderUid}")
    ResponseEntity<Object> getFolderBy(@PathVariable String folderUid);

    /**
     * Retrieves rule group information from a specific folder.
     *
     * @param folderUid - the unique identifier of the folder
     * @param group - the name of the rule group
     * @return ResponseEntity containing rule group details
     */
    @GetExchange("/api/v1/provisioning/folder/{folderUid}/rule-groups/{group}")
    ResponseEntity<Object> getRuleGroupBy(
            @PathVariable String folderUid, @PathVariable String group);

    /**
     * Retrieves organization information by its unique identifier.
     *
     * @param orgId - the unique identifier of the organization
     * @return ResponseEntity containing organization details
     */
    @GetExchange("/api/orgs/{orgId}")
    ResponseEntity<Object> getOrgBy(@PathVariable Long orgId);
}
