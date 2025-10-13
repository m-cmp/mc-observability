package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.port.InsightPort;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/insight")
public class InsightController {

    private final InsightPort insightPort;

    /* ===================== ANOMALY ===================== */
    @GetMapping("/anomaly-detection/measurement")
    public Object getMeasurements() {
        return insightPort.getMeasurements();
    }

    @GetMapping("/anomaly-detection/measurement/{measurement}")
    public Object getSpecificMeasurement(@PathVariable String measurement) {
        return insightPort.getSpecificMeasurement(measurement);
    }

    @GetMapping("/anomaly-detection/options")
    public Object getOptions() {
        return insightPort.getOptions();
    }

    @PostMapping("/anomaly-detection/{settingSeq}")
    public Object predictAnomalyDetection(@PathVariable int settingSeq, @RequestBody Object body) {
        return insightPort.predictAnomaly(settingSeq, body);
    }

    @GetMapping("/anomaly-detection/settings")
    public Object getAnomalySettings() {
        return insightPort.getAnomalySettings();
    }

    @PostMapping("/anomaly-detection/settings")
    public Object createAnomalySetting(@RequestBody Object body) {
        return insightPort.createAnomalySetting(body);
    }

    @PutMapping("/anomaly-detection/settings/{settingSeq}")
    public Object updateAnomalySetting(@PathVariable int settingSeq, @RequestBody Object body) {
        return insightPort.updateAnomalySetting(settingSeq, body);
    }

    @DeleteMapping("/anomaly-detection/settings/{settingSeq}")
    public Object deleteAnomalySetting(@PathVariable int settingSeq) {
        return insightPort.deleteAnomalySetting(settingSeq);
    }

    @GetMapping("/anomaly-detection/nsId/{nsId}/target/{targetId}/history")
    public Object getAnomalyHistory(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getAnomalyHistory(nsId, targetId, measurement, startTime, endTime);
    }

    /* ===================== ALERT ===================== */
    @PostMapping("/alert-analysis/query")
    public Object queryAlertAnalysis(@RequestBody Object body) {
        return insightPort.queryAlertAnalysis(body);
    }

    /* ===================== LLM ===================== */
    @GetMapping("/llm/model")
    public Object getLLMModelOptions() {
        return insightPort.getLLMModelOptions();
    }

    @GetMapping("/llm/session")
    public Object getLLMChatSessions() {
        return insightPort.getLLMChatSessions();
    }

    @PostMapping("/llm/session")
    public Object postLLMChatSession(@RequestBody Object body) {
        return insightPort.postLLMChatSession(body);
    }

    @DeleteMapping("/llm/session")
    public Object deleteLLMChatSession(@RequestParam String sessionId) {
        return insightPort.deleteLLMChatSession(sessionId);
    }

    @DeleteMapping("/llm/sessions")
    public Object deleteAllLLMChatSessions() {
        return insightPort.deleteAllLLMChatSessions();
    }

    @GetMapping("/llm/session/{sessionId}/history")
    public Object getLLMSessionHistory(@PathVariable String sessionId) {
        return insightPort.getLLMSessionHistory(sessionId);
    }

    @GetMapping("/llm/api-keys")
    public Object getLLMApiKeys(String provider) {
        return insightPort.getLLMApiKeys(provider);
    }

    @PostMapping("/llm/api-keys")
    public Object postLLMApiKeys(@RequestBody Object body) {
        return insightPort.postLLMApiKeys(body);
    }

    @DeleteMapping("/llm/api-Keys")
    public Object deleteLLMApiKeys(@RequestParam String provider) {
        return insightPort.deleteLLMApiKeys(provider);
    }

    /* ===================== LOG ===================== */
    @PostMapping("/log-analysis/query")
    public Object queryLogAnalysis(@RequestBody Object body) {
        return insightPort.queryLogAnalysis(body);
    }

    /* ===================== PREDICTION ===================== */
    @GetMapping("/predictions/measurement")
    public Object getPredictionMeasurements() {
        return insightPort.getPredictionMeasurements();
    }

    @GetMapping("/predictions/measurement/{measurement}")
    public Object getPredictionSpecificMeasurement(@PathVariable String measurement) {
        return insightPort.getPredictionSpecificMeasurement(measurement);
    }

    @GetMapping("/predictions/options")
    public Object getPredictionOptions() {
        return insightPort.getPredictionOptions();
    }

    @PostMapping("/predictions/nsId/{nsId}/target/{targetId}")
    public Object predictMonitoringData(
            @PathVariable String nsId, @PathVariable String targetId, @RequestBody Object body) {
        return insightPort.predictMonitoringData(nsId, targetId, body);
    }

    @GetMapping("/predictions/nsId/{nsId}/target/{targetId}/history")
    public Object getPredictionHistory(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getPredictionHistory(nsId, targetId, measurement, startTime, endTime);
    }
}
