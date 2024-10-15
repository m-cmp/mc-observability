package mcmp.mc.observability.mco11ymanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11ymanager.client.TriggerClient;
import mcmp.mc.observability.mco11ymanager.common.Constants;
import mcmp.mc.observability.mco11ymanager.service.UtilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.PREFIX_V1)
public class UtilController {
    private final TriggerClient triggerClient;
    private final UtilService utilService;

    @GetMapping("/readyz")
    @Operation(operationId = "GetReadyz", summary = "Check API health status",
            tags = "[Health] Check API Health")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", useReturnTypeSchema = true)})
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
