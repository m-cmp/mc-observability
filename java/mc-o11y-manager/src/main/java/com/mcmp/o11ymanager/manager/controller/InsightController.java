package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.port.InsightPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[Insight] Observability Intelligence")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/insight")
public class InsightController {

    private final InsightPort insightPort;

    /* ===================== ANOMALY ===================== */
    @Operation(summary = "GetAnomalyMeasurementList", operationId = "GetAnomalyMeasurementList", description = "Get measurements, field lists available for the feature")
    @GetMapping("/anomaly-detection/measurement")
    public Object getMeasurements() {
        return insightPort.getMeasurements();
    }

    @Operation(summary = "GetAnomalyFieldListByMeasurement", operationId = "GetAnomalyFieldListByMeasurement", description = "Get Field list of specific measurements available for that feature")
    @GetMapping("/anomaly-detection/measurement/{measurement}")
    public Object getSpecificMeasurement(@PathVariable String measurement) {
        return insightPort.getSpecificMeasurement(measurement);
    }

    @Operation(summary = "GetAnomalyDetectionOptions", operationId = "GetAnomalyDetectionOptions", description = "Fetch the available target types, metric types, and interval options for the anomaly detection API.")
    @GetMapping("/anomaly-detection/options")
    public Object getOptions() {
        return insightPort.getOptions();
    }

    @Operation(summary = "PostAnomalyDetection", operationId = "PostAnomalyDetection", description = "Request anomaly detection")
    @PostMapping("/anomaly-detection/{settingSeq}")
    public Object predictAnomalyDetection(@PathVariable int settingSeq) {
        return insightPort.predictAnomaly(settingSeq);
    }

    @Operation(summary = "GetAllAnomalyDetectionSettings", operationId = "GetAllAnomalyDetectionSettings", description = "Fetch the current settings for all anomaly detection targets.")
    @GetMapping("/anomaly-detection/settings")
    public Object getAnomalySettings() {
        return insightPort.getAnomalySettings();
    }

    @Operation(summary = "PostAnomalyDetectionSettings", operationId = "PostAnomalyDetectionSettings", description = "Register a target for anomaly detection and automatically schedule detection tasks.")
    @PostMapping("/anomaly-detection/settings")
    public Object createAnomalySetting(@RequestBody Object body) {
        return insightPort.createAnomalySetting(body);
    }

    @Operation(summary = "PutAnomalyDetectionSettings", operationId = "PutAnomalyDetectionSettings", description = "Modify the settings for a specific anomaly detection target, including the monitoring metric and interval.")
    @PutMapping("/anomaly-detection/settings/{settingSeq}")
    public Object updateAnomalySetting(@PathVariable int settingSeq, @RequestBody Object body) {
        return insightPort.updateAnomalySetting(settingSeq, body);
    }

    @Operation(summary = "DeleteAnomalyDetectionSettings", operationId = "DeleteAnomalyDetectionSettings", description = "Remove a target from anomaly detection, stopping and removing any scheduled tasks.")
    @DeleteMapping("/anomaly-detection/settings/{settingSeq}")
    public Object deleteAnomalySetting(@PathVariable int settingSeq) {
        return insightPort.deleteAnomalySetting(settingSeq);
    }

    @Operation(summary = "GetMCIAnomalyDetectionSettings", operationId = "GetMCIAnomalyDetectionSettings", description = "Fetch the current anomaly detection settings for a specific mci group.")
    @GetMapping("/anomaly-detection/settings/ns/{nsId}/mci/{mciId}")
    public Object getAnomalySettingsForMci(@PathVariable String nsId, @PathVariable String mciId) {
        return insightPort.getAnomalySettingsForMci(nsId, mciId);
    }

    @Operation(summary = "GetVMAnomalyDetectionSettings", operationId = "GetVMAnomalyDetectionSettings", description = "Fetch the current anomaly detection settings for a specific vm.")
    @GetMapping("/anomaly-detection/settings/ns/{nsId}/mci/{mciId}/vm/{vmId}")
    public Object getAnomalySettingsForVm(
            @PathVariable String nsId, @PathVariable String mciId, @PathVariable String vmId) {
        return insightPort.getAnomalySettingsForVm(nsId, mciId, vmId);
    }

    @Operation(summary = "GetAnomalyDetectionMCIHistory", operationId = "GetAnomalyDetectionMCIHistory", description = "Fetch the results of anomaly detection for mci group within a given time range.")
    @GetMapping("/anomaly-detection/ns/{nsId}/mci/{mciId}/history")
    public Object getAnomalyHistoryForMci(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getAnomalyHistoryForMci(nsId, mciId, measurement, startTime, endTime);
    }

    @Operation(summary = "GetAnomalyDetectionVMHistory", operationId = "GetAnomalyDetectionVMHistory", description = "Fetch the results of anomaly detection for a specific vm within a given time range.")
    @GetMapping("/anomaly-detection/ns/{nsId}/mci/{mciId}/vm/{vmId}/history")
    public Object getAnomalyHistoryForVm(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String vmId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getAnomalyHistoryForVm(
                nsId, mciId, vmId, measurement, startTime, endTime);
    }

    /* ===================== ALERT ===================== */
    @Operation(summary = "PostAlertAnalysisQuery", operationId = "PostAlertAnalysisQuery", description = "Submit a query to the alert analysis chat session for intelligent alert investigation.")
    @PostMapping("/alert-analysis/query")
    public Object queryAlertAnalysis(@RequestBody Object body) {
        return insightPort.queryAlertAnalysis(body);
    }

    /* ===================== LLM ===================== */
    @Operation(summary = "GetLLMModelOptions", operationId = "GetLLMModelOptions", description = "Retrieve available LLM model options and configurations for log analysis.")
    @GetMapping("/llm/model")
    public Object getLLMModelOptions() {
        return insightPort.getLLMModelOptions();
    }

    @Operation(summary = "GetLLMChatSessions", operationId = "GetLLMChatSessions", description = "Retrieve all active LLM chat sessions for log analysis.")
    @GetMapping("/llm/session")
    public Object getLLMChatSessions() {
        return insightPort.getLLMChatSessions();
    }

    @Operation(summary = "PostLLMChatSession", operationId = "PostLLMChatSession", description = "Create a new LLM chat session for log analysis with specified provider and model.")
    @PostMapping("/llm/session")
    public Object postLLMChatSession(@RequestBody Object body) {
        return insightPort.postLLMChatSession(body);
    }

    @Operation(summary = "DeleteLLMChatSession", operationId = "DeleteLLMChatSession", description = "Delete a specific LLM chat session and its conversation history.")
    @DeleteMapping("/llm/session")
    public Object deleteLLMChatSession(@RequestParam String sessionId) {
        return insightPort.deleteLLMChatSession(sessionId);
    }

    @Operation(summary = "DeleteAllLLMChatSessions", operationId = "DeleteAllLLMChatSessions", description = "Delete all LLM chat sessions and their conversation histories.")
    @DeleteMapping("/llm/sessions")
    public Object deleteAllLLMChatSessions() {
        return insightPort.deleteAllLLMChatSessions();
    }

    @Operation(summary = "GetLLMSessionHistory", operationId = "GetLLMSessionHistory", description = "Retrieve the conversation history for a specific LLM chat session.")
    @GetMapping("/llm/session/{sessionId}/history")
    public Object getLLMSessionHistory(@PathVariable String sessionId) {
        return insightPort.getLLMSessionHistory(sessionId);
    }

    @Operation(summary = "GetLLMAPIKeys", operationId = "GetLLMAPIKeys", description = "Retrieve the current API key configuration.")
    @GetMapping("/llm/api-keys")
    public Object getLLMApiKeys(String provider) {
        return insightPort.getLLMApiKeys(provider);
    }

    @Operation(summary = "PostLLMAPIKeys", operationId = "PostLLMAPIKeys", description = "Save or update the API key configuration.")
    @PostMapping("/llm/api-keys")
    public Object postLLMApiKeys(@RequestBody Object body) {
        return insightPort.postLLMApiKeys(body);
    }

    @Operation(summary = "DeleteLLMAPIKeys", operationId = "DeleteLLMAPIKeys", description = "Delete the API key configuration.")
    @DeleteMapping("/llm/api-Keys")
    public Object deleteLLMApiKeys(@RequestParam String provider) {
        return insightPort.deleteLLMApiKeys(provider);
    }

    /* ===================== LOG ===================== */
    @Operation(summary = "PostLogAnalysisQuery", operationId = "PostLogAnalysisQuery", description = "Submit a query to the log analysis chat session for intelligent log investigation and troubleshooting.")
    @PostMapping("/log-analysis/query")
    public Object queryLogAnalysis(@RequestBody Object body) {
        return insightPort.queryLogAnalysis(body);
    }

    /* ===================== PREDICTION ===================== */
    @Operation(summary = "GetPredictionMeasurementList", operationId = "GetPredictionMeasurementList", description = "Get measurements, field lists available for the feature")
    @GetMapping("/predictions/measurement")
    public Object getPredictionMeasurements() {
        return insightPort.getPredictionMeasurements();
    }

    @Operation(summary = "GetPredictionFieldListByMeasurement", operationId = "GetPredictionFieldListByMeasurement", description = "Get Field list of specific measurement available for that feature")
    @GetMapping("/predictions/measurement/{measurement}")
    public Object getPredictionSpecificMeasurement(@PathVariable String measurement) {
        return insightPort.getPredictionSpecificMeasurement(measurement);
    }

    @Operation(summary = "GetPredictionOptions", operationId = "GetPredictionOptions", description = "Fetch the available target types, metric types, and prediction range options for the prediction API.")
    @GetMapping("/predictions/options")
    public Object getPredictionOptions() {
        return insightPort.getPredictionOptions();
    }

    @Operation(summary = "PostPredictionMCI", operationId = "PostPredictionMCI", description = "Predict future metrics (cpu, mem, disk, system) for a given mci group.")
    @PostMapping("/predictions/ns/{nsId}/mci/{mciId}")
    public Object predictMonitoringDataForMci(
            @PathVariable String nsId, @PathVariable String mciId, @RequestBody Object body) {
        return insightPort.predictMonitoringDataForMci(nsId, mciId, body);
    }

    @Operation(summary = "PostPredictionVM", operationId = "PostPredictionVM", description = "Predict future metrics (cpu, mem, disk, system) for a given vm.")
    @PostMapping("/predictions/ns/{nsId}/mci/{mciId}/vm/{vmId}")
    public Object predictMonitoringDataForVm(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String vmId,
            @RequestBody Object body) {
        return insightPort.predictMonitoringDataForVm(nsId, mciId, vmId, body);
    }

    @Operation(summary = "GetPredictionMCIHistory", operationId = "GetPredictionMCIHistory", description = "Get previously stored prediction data for a specific mci group.")
    @GetMapping("/predictions/ns/{nsId}/mci/{mciId}/history")
    public Object getPredictionHistoryForMci(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getPredictionHistoryForMci(nsId, mciId, measurement, startTime, endTime);
    }

    @Operation(summary = "GetPredictionVMHistory", operationId = "GetPredictionVMHistory", description = "Get previously stored prediction data for a specific vm.")
    @GetMapping("/predictions/ns/{nsId}/mci/{mciId}/vm/{vmId}/history")
    public Object getPredictionHistoryForVm(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String vmId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getPredictionHistoryForVm(
                nsId, mciId, vmId, measurement, startTime, endTime);
    }
}
