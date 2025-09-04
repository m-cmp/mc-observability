package com.mcmp.o11ymanager.trigger.adapter.external.alert;

import com.mcmp.o11ymanager.trigger.adapter.external.alert.dto.AlertRuleCreateDto;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.GrafanaHealthCheckResponse;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.contact.AlertConfig.GrafanaManagedReceiverConfig;
import java.util.List;

public interface AlertManager {
    void createAlertRule(AlertRuleCreateDto dto, String datasourceUid);

    void deleteAlertRule(String uuid);

    GrafanaHealthCheckResponse checkGrafanaHealth();

    Object getAllAlerts();

    Object getAlertBy(String title);

    Object getAllAlertRules();

    List<GrafanaManagedReceiverConfig> getAllContactPoints();

    void testAlertReceiver();
}
