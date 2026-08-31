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
    @Operation(
            summary = "GetAnomalyMeasurementList",
            operationId = "GetAnomalyMeasurementList",
            description = "Get measurements, field lists available for the feature")
    @GetMapping("/anomaly-detection/measurement")
    public Object getMeasurements() {
        return insightPort.getMeasurements();
    }

    @Operation(
            summary = "GetAnomalyFieldListByMeasurement",
            operationId = "GetAnomalyFieldListByMeasurement",
            description = "Get Field list of specific measurements available for that feature")
    @GetMapping("/anomaly-detection/measurement/{measurement}")
    public Object getSpecificMeasurement(@PathVariable String measurement) {
        return insightPort.getSpecificMeasurement(measurement);
    }

    @Operation(
            summary = "GetAnomalyDetectionOptions",
            operationId = "GetAnomalyDetectionOptions",
            description =
                    "Fetch the available target types, metric types, and interval options for the anomaly detection API.")
    @GetMapping("/anomaly-detection/options")
    public Object getOptions() {
        return insightPort.getOptions();
    }

    @Operation(
            summary = "PostAnomalyDetection",
            operationId = "PostAnomalyDetection",
            description = "Request anomaly detection")
    @PostMapping("/anomaly-detection/{settingSeq}")
    public Object predictAnomalyDetection(@PathVariable int settingSeq) {
        return insightPort.predictAnomaly(settingSeq);
    }

    @Operation(
            summary = "GetAllAnomalyDetectionSettings",
            operationId = "GetAllAnomalyDetectionSettings",
            description = "Fetch the current settings for all anomaly detection targets.")
    @GetMapping("/anomaly-detection/settings")
    public Object getAnomalySettings() {
        return insightPort.getAnomalySettings();
    }

    @Operation(
            summary = "PostAnomalyDetectionSettings",
            operationId = "PostAnomalyDetectionSettings",
            description =
                    "Register a target for anomaly detection and automatically schedule detection tasks.")
    @PostMapping("/anomaly-detection/settings")
    public Object createAnomalySetting(@RequestBody Object body) {
        return insightPort.createAnomalySetting(body);
    }

    @Operation(
            summary = "PutAnomalyDetectionSettings",
            operationId = "PutAnomalyDetectionSettings",
            description =
                    "Modify the settings for a specific anomaly detection target, including the monitoring metric and interval.")
    @PutMapping("/anomaly-detection/settings/{settingSeq}")
    public Object updateAnomalySetting(@PathVariable int settingSeq, @RequestBody Object body) {
        return insightPort.updateAnomalySetting(settingSeq, body);
    }

    @Operation(
            summary = "DeleteAnomalyDetectionSettings",
            operationId = "DeleteAnomalyDetectionSettings",
            description =
                    "Remove a target from anomaly detection, stopping and removing any scheduled tasks.")
    @DeleteMapping("/anomaly-detection/settings/{settingSeq}")
    public Object deleteAnomalySetting(@PathVariable int settingSeq) {
        return insightPort.deleteAnomalySetting(settingSeq);
    }

    @Operation(
            summary = "GetMCIAnomalyDetectionSettings",
            operationId = "GetMCIAnomalyDetectionSettings",
            description = "Fetch the current anomaly detection settings for a specific mci group.")
    @GetMapping("/anomaly-detection/settings/ns/{nsId}/infra/{infraId}")
    public Object getAnomalySettingsForInfra(
            @PathVariable String nsId, @PathVariable String infraId) {
        return insightPort.getAnomalySettingsForInfra(nsId, infraId);
    }

    @Operation(
            summary = "GetVMAnomalyDetectionSettings",
            operationId = "GetVMAnomalyDetectionSettings",
            description = "Fetch the current anomaly detection settings for a specific vm.")
    @GetMapping("/anomaly-detection/settings/ns/{nsId}/infra/{infraId}/node/{nodeId}")
    public Object getAnomalySettingsForNode(
            @PathVariable String nsId, @PathVariable String infraId, @PathVariable String nodeId) {
        return insightPort.getAnomalySettingsForNode(nsId, infraId, nodeId);
    }

    @Operation(
            summary = "GetAnomalyDetectionMCIHistory",
            operationId = "GetAnomalyDetectionMCIHistory",
            description =
                    "Fetch the results of anomaly detection for mci group within a given time range.")
    @GetMapping("/anomaly-detection/ns/{nsId}/infra/{infraId}/history")
    public Object getAnomalyHistoryForInfra(
            @PathVariable String nsId,
            @PathVariable String infraId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getAnomalyHistoryForInfra(
                nsId, infraId, measurement, startTime, endTime);
    }

    @Operation(
            summary = "GetAnomalyDetectionVMHistory",
            operationId = "GetAnomalyDetectionVMHistory",
            description =
                    "Fetch the results of anomaly detection for a specific vm within a given time range.")
    @GetMapping("/anomaly-detection/ns/{nsId}/infra/{infraId}/node/{nodeId}/history")
    public Object getAnomalyHistoryForNode(
            @PathVariable String nsId,
            @PathVariable String infraId,
            @PathVariable String nodeId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getAnomalyHistoryForNode(
                nsId, infraId, nodeId, measurement, startTime, endTime);
    }

    /* ===================== ALERT ===================== */
    @Operation(
            summary = "PostAlertAnalysisQuery",
            operationId = "PostAlertAnalysisQuery",
            description =
                    "Submit a query to the alert analysis chat session for intelligent alert investigation.")
    @PostMapping("/alert-analysis/query")
    public Object queryAlertAnalysis(@RequestBody Object body) {
        return insightPort.queryAlertAnalysis(body);
    }

    /* ===================== LLM ===================== */
    @GetMapping("/llm/connections")
    public Object getLLMConnections() {
        return insightPort.getLLMConnections();
    }

    @GetMapping("/llm/connections/{connectionId}")
    public Object getLLMConnection(@PathVariable int connectionId) {
        return insightPort.getLLMConnection(connectionId);
    }

    @PostMapping("/llm/connections")
    public Object postLLMConnection(@RequestBody Object body) {
        return insightPort.postLLMConnection(body);
    }

    @PatchMapping("/llm/connections/{connectionId}")
    public Object patchLLMConnection(@PathVariable int connectionId, @RequestBody Object body) {
        return insightPort.patchLLMConnection(connectionId, body);
    }

    @DeleteMapping("/llm/connections/{connectionId}")
    public Object deleteLLMConnection(@PathVariable int connectionId) {
        return insightPort.deleteLLMConnection(connectionId);
    }

    @GetMapping("/llm/connections/{connectionId}/models")
    public Object getLLMConnectionModels(@PathVariable int connectionId) {
        return insightPort.getLLMConnectionModels(connectionId);
    }

    @PutMapping("/llm/connections/{connectionId}/default")
    public Object setDefaultLLMConnection(
            @PathVariable int connectionId, @RequestBody Object body) {
        return insightPort.setDefaultLLMConnection(connectionId, body);
    }

    @Operation(
            summary = "GetLLMChatSessions",
            operationId = "GetLLMChatSessions",
            description = "Retrieve all active LLM chat sessions for log analysis.")
    @GetMapping("/llm/sessions")
    public Object getLLMChatSessions() {
        return insightPort.getLLMChatSessions();
    }

    @Operation(
            summary = "PostLLMChatSession",
            operationId = "PostLLMChatSession",
            description = "Create a new LLM chat session with a configured connection and model.")
    @PostMapping("/llm/sessions")
    public Object postLLMChatSession(@RequestBody Object body) {
        return insightPort.postLLMChatSession(body);
    }

    @Operation(
            summary = "DeleteLLMChatSession",
            operationId = "DeleteLLMChatSession",
            description = "Delete a specific LLM chat session and its conversation history.")
    @DeleteMapping("/llm/sessions/{sessionId}")
    public Object deleteLLMChatSession(@PathVariable String sessionId) {
        return insightPort.deleteLLMChatSession(sessionId);
    }

    @Operation(
            summary = "DeleteAllLLMChatSessions",
            operationId = "DeleteAllLLMChatSessions",
            description = "Delete all LLM chat sessions and their conversation histories.")
    @DeleteMapping("/llm/sessions")
    public Object deleteAllLLMChatSessions() {
        return insightPort.deleteAllLLMChatSessions();
    }

    @Operation(
            summary = "GetLLMSessionHistory",
            operationId = "GetLLMSessionHistory",
            description = "Retrieve the conversation history for a specific LLM chat session.")
    @GetMapping("/llm/sessions/{sessionId}/history")
    public Object getLLMSessionHistory(@PathVariable String sessionId) {
        return insightPort.getLLMSessionHistory(sessionId);
    }

    /* ===================== LOG ===================== */
    @Operation(
            summary = "PostLogAnalysisQuery",
            operationId = "PostLogAnalysisQuery",
            description =
                    "Submit a query to the log analysis chat session for intelligent log investigation and troubleshooting.")
    @PostMapping("/log-analysis/query")
    public Object queryLogAnalysis(@RequestBody Object body) {
        return insightPort.queryLogAnalysis(body);
    }

    /* ===================== PREDICTION ===================== */
    @Operation(
            summary = "GetPredictionMeasurementList",
            operationId = "GetPredictionMeasurementList",
            description = "Get measurements, field lists available for the feature")
    @GetMapping("/predictions/measurement")
    public Object getPredictionMeasurements() {
        return insightPort.getPredictionMeasurements();
    }

    @Operation(
            summary = "GetPredictionFieldListByMeasurement",
            operationId = "GetPredictionFieldListByMeasurement",
            description = "Get Field list of specific measurement available for that feature")
    @GetMapping("/predictions/measurement/{measurement}")
    public Object getPredictionSpecificMeasurement(@PathVariable String measurement) {
        return insightPort.getPredictionSpecificMeasurement(measurement);
    }

    @Operation(
            summary = "GetPredictionOptions",
            operationId = "GetPredictionOptions",
            description =
                    "Fetch the available target types, metric types, and prediction range options for the prediction API.")
    @GetMapping("/predictions/options")
    public Object getPredictionOptions() {
        return insightPort.getPredictionOptions();
    }

    @Operation(
            summary = "PostPredictionMCI",
            operationId = "PostPredictionMCI",
            description = "Predict future metrics (cpu, mem, disk, system) for a given mci group.")
    @PostMapping("/predictions/ns/{nsId}/infra/{infraId}")
    public Object predictMonitoringDataForInfra(
            @PathVariable String nsId, @PathVariable String infraId, @RequestBody Object body) {
        return insightPort.predictMonitoringDataForInfra(nsId, infraId, body);
    }

    @Operation(
            summary = "PostPredictionVM",
            operationId = "PostPredictionVM",
            description = "Predict future metrics (cpu, mem, disk, system) for a given vm.")
    @PostMapping("/predictions/ns/{nsId}/infra/{infraId}/node/{nodeId}")
    public Object predictMonitoringDataForNode(
            @PathVariable String nsId,
            @PathVariable String infraId,
            @PathVariable String nodeId,
            @RequestBody Object body) {
        return insightPort.predictMonitoringDataForNode(nsId, infraId, nodeId, body);
    }

    @Operation(
            summary = "GetPredictionMCIHistory",
            operationId = "GetPredictionMCIHistory",
            description = "Get previously stored prediction data for a specific mci group.")
    @GetMapping("/predictions/ns/{nsId}/infra/{infraId}/history")
    public Object getPredictionHistoryForInfra(
            @PathVariable String nsId,
            @PathVariable String infraId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getPredictionHistoryForInfra(
                nsId, infraId, measurement, startTime, endTime);
    }

    @Operation(
            summary = "GetPredictionVMHistory",
            operationId = "GetPredictionVMHistory",
            description = "Get previously stored prediction data for a specific vm.")
    @GetMapping("/predictions/ns/{nsId}/infra/{infraId}/node/{nodeId}/history")
    public Object getPredictionHistoryForNode(
            @PathVariable String nsId,
            @PathVariable String infraId,
            @PathVariable String nodeId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getPredictionHistoryForNode(
                nsId, infraId, nodeId, measurement, startTime, endTime);
    }

    /* ===================== RCA ===================== */
    @PostMapping("/rca/query")
    public Object queryRca(@RequestBody Object body) {
        return insightPort.queryRca(body);
    }

    @GetMapping("/rca/records")
    public Object listRcaRecords(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) String fromDt,
            @RequestParam(value = "to", required = false) String toDt,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return insightPort.listRcaRecords(status, fromDt, toDt, page, size);
    }

    @GetMapping("/rca/records/{analysisId}")
    public Object getRcaRecord(@PathVariable int analysisId) {
        return insightPort.getRcaRecord(analysisId);
    }

    @GetMapping("/rca/schedules")
    public Object listRcaSchedules() {
        return insightPort.listRcaSchedules();
    }

    @PostMapping("/rca/schedules")
    public Object postRcaSchedule(@RequestBody Object body) {
        return insightPort.postRcaSchedule(body);
    }

    @PatchMapping("/rca/schedules/{scheduleId}")
    public Object patchRcaSchedule(@PathVariable int scheduleId, @RequestBody Object body) {
        return insightPort.patchRcaSchedule(scheduleId, body);
    }

    @DeleteMapping("/rca/schedules/{scheduleId}")
    public Object deleteRcaSchedule(@PathVariable int scheduleId) {
        return insightPort.deleteRcaSchedule(scheduleId);
    }
}
