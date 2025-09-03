package com.mcmp.o11ymanager.trigger.application.controller;

import com.mcmp.o11ymanager.trigger.adapter.external.alert.AlertManager;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.AlertTestHistoryPageResponse;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.GrafanaHealthCheckResponse;
import com.mcmp.o11ymanager.trigger.application.service.AlertService;
import com.mcmp.o11ymanager.trigger.application.service.dto.AlertTestHistoryDetailDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model.contact.AlertConfig.GrafanaManagedReceiverConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for alert management Provides integration with Grafana alert system, manages
 * alert rules, contact points, and testing functionalities.
 */
@RestController
@RequestMapping("/api/v1/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertManager alertManager;
    private final AlertService alertService;

    /**
     * Checks the health status of Grafana server.
     *
     * @return Grafana server health status information
     */
    @GetMapping("/health")
    public ResponseEntity<GrafanaHealthCheckResponse> checkGrafanaHealth() {
        GrafanaHealthCheckResponse healthStatus = alertManager.checkGrafanaHealth();
        return ResponseEntity.ok(healthStatus);
    }

    /**
     * Retrieves all alerts list.
     *
     * @return List of alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<Object> getAllAlerts() {
        Object alerts = alertManager.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Searches for alerts by title.
     *
     * @param title Title of the alert to search
     * @return Found alert information
     */
    @GetMapping("/alerts/search")
    public ResponseEntity<Object> getAlertBy(@RequestParam String title) {
        Object alert = alertManager.getAlertBy(title);
        return ResponseEntity.ok(alert);
    }

    /**
     * Retrieves all alert rules list.
     *
     * @return List of alert rules
     */
    @GetMapping("/alert-rules")
    public ResponseEntity<Object> getAllAlertRules() {
        Object alertRules = alertManager.getAllAlertRules();
        return ResponseEntity.ok(alertRules);
    }

    /**
     * Retrieves all contact points list.
     *
     * @return List of contact points
     */
    @GetMapping("/contact-points")
    public ResponseEntity<List<GrafanaManagedReceiverConfig>> getAllContactPoints() {
        List<GrafanaManagedReceiverConfig> contactPoints = alertManager.getAllContactPoints();
        return ResponseEntity.ok(contactPoints);
    }

    /**
     * Tests the alert receiver.
     *
     * @return Test completion response
     */
    @PostMapping("/alert-receiver/test")
    public ResponseEntity<Void> testAlertReceiver() {
        alertManager.testAlertReceiver();
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves alert test histories with pagination.
     *
     * @param page Page number (default: 1)
     * @param size Page size (default: 10)
     * @param sortBy Field to sort by (default: id)
     * @param direction Sort direction (default: desc)
     * @return Alert test history list with paging information
     */
    @GetMapping("/test-history")
    public ResponseEntity<AlertTestHistoryPageResponse> getAlertTestHistories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sortDirection, sortBy));
        CustomPageDto<AlertTestHistoryDetailDto> alertTestHistories =
                alertService.getAlertTestHistories(pageable);
        return ResponseEntity.ok(AlertTestHistoryPageResponse.from(alertTestHistories));
    }
}
