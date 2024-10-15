package mcmp.mc.observability.mco11yagent.util.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.util.service.UtilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y")
public class UtilController {
    private final UtilService utilService;

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, Object>> readyz() {
        Map<String, Object> healthStatus = utilService.checkApiHealth();
        String status = (String) healthStatus.get("status");

        if ("DOWN".equals(status)) {
            return ResponseEntity.status(503).body(healthStatus);
        } else {
            return ResponseEntity.ok(healthStatus);
        }
    }
}
