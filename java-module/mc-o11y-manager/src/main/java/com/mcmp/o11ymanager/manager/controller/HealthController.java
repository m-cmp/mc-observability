package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.service.HealthServiceImpl;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y")
public class HealthController {
    private final HealthServiceImpl healthService;

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, Object>> readyz() {
        Map<String, Object> healthStatus = healthService.checkApiHealth();
        String status = (String) healthStatus.get("status");

        if ("DOWN".equals(status)) {
            return ResponseEntity.status(503).body(healthStatus);
        } else {
            return ResponseEntity.ok(healthStatus);
        }
    }
}
