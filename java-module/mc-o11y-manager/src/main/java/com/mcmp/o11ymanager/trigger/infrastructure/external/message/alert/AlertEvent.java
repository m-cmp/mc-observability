package com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert;

import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.application.common.type.AlertLevel;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.message.GrafanaAlert;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.message.GrafanaAlertMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Alert event data class that aggregates alert details by severity level Represents an alert event
 * containing multiple alerts categorized by INFO, WARNING, and CRITICAL levels.
 */
@Getter
public class AlertEvent {
    private String title;
    private final List<AlertDetail> infoAlerts = new ArrayList<>();
    private final List<AlertDetail> warningAlerts = new ArrayList<>();
    private final List<AlertDetail> criticalAlerts = new ArrayList<>();
    private final Map<String, AlertDetail> alertDetailMap = new HashMap<>();

    /** Default constructor for AlertEvent. */
    public AlertEvent() {}

    /**
     * Constructor for AlertEvent with title.
     *
     * @param title the title of the alert event
     */
    public AlertEvent(String title) {
        this.title = title;
    }

    /**
     * Creates an AlertEvent from Grafana alert message and threshold conditions.
     *
     * @param grafanaAlertMessage the Grafana alert message containing alert details
     * @param thresholdCondition threshold conditions for determining alert levels
     * @return AlertEvent populated with alert details categorized by severity
     */
    public static AlertEvent from(
            GrafanaAlertMessage grafanaAlertMessage, ThresholdCondition thresholdCondition) {
        AlertEvent alertEvent = new AlertEvent();
        alertEvent.title = grafanaAlertMessage.getCommonLabels().get("ruleGroup");
        List<GrafanaAlert> alerts = grafanaAlertMessage.getAlerts();

        alerts.forEach(
                alert -> {
                    if (!isResolvedAlert(alert) && !isAlertByRuleDeleted(alert)) {
                        Map<String, String> labels = alert.getLabels();
                        Map<String, Double> values = alert.getValues();
                        AlertLevel alertLevel = AlertLevel.valueOf(values.get("C").intValue());
                        String resourceUsage = String.valueOf(values.get("B"));
                        if (resourceUsage.length() > 4) {
                            resourceUsage = resourceUsage.substring(0, 4);
                        }

                        AlertDetail alertDetail =
                                AlertDetail.builder()
                                        .namespaceId(labels.get("ns_id"))
                                        .mciId(labels.get("mci_id"))
                                        .targetId(labels.get("vm_id"))
                                        .resourceType(labels.get("resource_type"))
                                        .resourceUsage(resourceUsage)
                                        .status(alert.getStatus())
                                        .threshold(
                                                String.valueOf(
                                                        getThreshold(
                                                                alertLevel, thresholdCondition)))
                                        .alertLevel(alertLevel.name())
                                        .startsAt(alert.getStartsAt())
                                        .build();

                        String alertKey =
                                labels.get("ns_id") + labels.get("mci_id") + labels.get("vm_id");
                        if (alertEvent.alertDetailMap.containsKey(alertKey)) {
                            AlertDetail savedDetail = alertEvent.alertDetailMap.get(alertKey);
                            AlertLevel savedLevel = AlertLevel.findBy(savedDetail.alertLevel);
                            AlertLevel level = AlertLevel.findBy(alertDetail.alertLevel);
                            if (savedLevel.getValue() < level.getValue()) {
                                alertEvent.alertDetailMap.put(alertKey, alertDetail);
                            }
                        } else {
                            alertEvent.alertDetailMap.put(alertKey, alertDetail);
                        }
                    }
                });

        for (AlertDetail alertDetail : alertEvent.alertDetailMap.values()) {
            AlertLevel alertLevel = AlertLevel.findBy(alertDetail.alertLevel);
            switch (alertLevel) {
                case INFO -> alertEvent.infoAlerts.add(alertDetail);
                case WARNING -> alertEvent.warningAlerts.add(alertDetail);
                case CRITICAL -> alertEvent.criticalAlerts.add(alertDetail);
            }
        }

        return alertEvent;
    }

    /**
     * Checks if the alert has been resolved.
     *
     * @param alert the Grafana alert to check
     * @return true if the alert status is "resolved"
     */
    private static boolean isResolvedAlert(GrafanaAlert alert) {
        return alert.getStatus().equals("resolved");
    }

    /**
     * Checks if the alert was triggered by a deleted rule.
     *
     * @param alert the Grafana alert to check
     * @return true if the alert was caused by a deleted rule
     */
    private static boolean isAlertByRuleDeleted(GrafanaAlert alert) {
        Map<String, String> annotations = alert.getAnnotations();
        return annotations.containsKey("grafana_state_reason")
                && annotations.get("grafana_state_reason").equals("RuleDeleted");
    }

    /**
     * Gets the threshold value for the specified alert level.
     *
     * @param alertLevel the alert level (INFO, WARNING, CRITICAL)
     * @param thresholdCondition threshold conditions containing level-specific thresholds
     * @return threshold value for the alert level
     */
    private static double getThreshold(
            AlertLevel alertLevel, ThresholdCondition thresholdCondition) {
        return switch (alertLevel) {
            case INFO -> thresholdCondition.info();
            case WARNING -> thresholdCondition.warning();
            case CRITICAL -> thresholdCondition.critical();
        };
    }

    /**
     * Adds INFO level alerts to the event.
     *
     * @param infoAlerts list of INFO level alert details to add
     */
    public void addInfoAlerts(List<AlertDetail> infoAlerts) {
        this.infoAlerts.addAll(infoAlerts);
    }

    /**
     * Adds WARNING level alerts to the event.
     *
     * @param warningAlerts list of WARNING level alert details to add
     */
    public void addWarningAlerts(List<AlertDetail> warningAlerts) {
        this.warningAlerts.addAll(warningAlerts);
    }

    /**
     * Adds CRITICAL level alerts to the event.
     *
     * @param criticalAlerts list of CRITICAL level alert details to add
     */
    public void addCriticalAlerts(List<AlertDetail> criticalAlerts) {
        this.criticalAlerts.addAll(criticalAlerts);
    }

    /**
     * Gets the total count of all alerts in this event.
     *
     * @return total number of alerts across all severity levels
     */
    public int getAlertsCount() {
        return infoAlerts.size() + warningAlerts.size() + criticalAlerts.size();
    }

    /**
     * Checks if the alert event contains no alerts.
     *
     * @return true if there are no alerts at any severity level
     */
    public boolean isEmpty() {
        return infoAlerts.isEmpty() && warningAlerts.isEmpty() && criticalAlerts.isEmpty();
    }

    /**
     * Alert detail data class containing specific information about an individual alert Includes
     * resource identification, threshold information, and alert metadata.
     */
    @Getter
    @Builder
    public static class AlertDetail {
        private String namespaceId;
        private String resourceType;
        private String mciId;
        private String targetId;
        private String alertLevel;
        private String threshold;
        private String resourceUsage;
        private String status;
        private String startsAt;
    }
}
