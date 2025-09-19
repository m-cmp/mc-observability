package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.AnomalyDetectionMeasurement;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.AnomalyDetectionOptions;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionBody;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionHistory;
import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.PredictionResult;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.LLMChatSession;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.LLMModel;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.Message;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.PostQueryBody;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.PostSessionBody;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.SessionHistory;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.PredictionMeasurement;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.PredictionOptions;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.infrastructure.insight.InsightClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/insight")
public class InsightController {

    private final InsightClient insightClient;

    /* ===================== ANOMALY ===================== */
    @GetMapping("/anomaly-detection/measurement")
    public ResBody<List<AnomalyDetectionMeasurement>> getMeasurements() {
        return insightClient.getMeasurements();
    }

    @GetMapping("/anomaly-detection/measurement/{measurement}")
    public ResBody<AnomalyDetectionMeasurement> getSpecificMeasurement(
            @PathVariable String measurement) {
        return insightClient.getSpecificMeasurement(measurement);
    }

    @GetMapping("/anomaly-detection/options")
    public ResBody<AnomalyDetectionOptions> getOptions() {
        return insightClient.getOptions();
    }

    @PostMapping("/anomaly-detection/nsId/{nsId}/target/{targetId}")
    public ResBody<PredictionResult> predictMetric(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestBody PredictionBody body) {
        return insightClient.predictMetric(nsId, targetId, body);
    }

    @GetMapping("/anomaly-detection/nsId/{nsId}/target/{targetId}/history")
    public ResBody<PredictionHistory> getAnomalyHistory(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightClient.getAnomalyHistory(nsId, targetId, measurement, startTime, endTime);
    }

    /* ===================== ALERT ===================== */
    @PostMapping("/alert-analysis/query")
    public ResBody<Message> queryAlertAnalysis(@RequestBody PostQueryBody body) {
        return insightClient.queryAlertAnalysis(body);
    }

    /* ===================== LLM ===================== */
    @GetMapping("/llm/model")
    public ResBody<List<LLMModel>> getLLMModelOptions() {
        return insightClient.getLLMModelOptions();
    }

    @GetMapping("/llm/session")
    public ResBody<List<LLMChatSession>> getLLMChatSessions() {
        return insightClient.getLLMChatSessions();
    }

    @PostMapping("/llm/session")
    public ResBody<LLMChatSession> postLLMChatSession(@RequestBody PostSessionBody body) {
        return insightClient.postLLMChatSession(body);
    }

    @DeleteMapping("/llm/session")
    public ResBody<LLMChatSession> deleteLLMChatSession(@RequestParam String sessionId) {
        return insightClient.deleteLLMChatSession(sessionId);
    }

    @DeleteMapping("/llm/sessions")
    public ResBody<List<LLMChatSession>> deleteAllLLMChatSessions() {
        return insightClient.deleteAllLLMChatSessions();
    }

    @GetMapping("/llm/session/{sessionId}/history")
    public ResBody<SessionHistory> getLLMSessionHistory(@PathVariable String sessionId) {
        return insightClient.getLLMSessionHistory(sessionId);
    }

    /* ===================== LOG ===================== */
    @PostMapping("/log-analysis/query")
    public ResBody<Message> queryLogAnalysis(@RequestBody PostQueryBody body) {
        return insightClient.queryLogAnalysis(body);
    }

    /* ===================== PREDICTION ===================== */
    @GetMapping("/predictions/measurement")
    public ResBody<List<PredictionMeasurement>> getPredictionMeasurements() {
        return insightClient.getPredictionMeasurements();
    }

    @GetMapping("/predictions/measurement/{measurement}")
    public ResBody<PredictionMeasurement> getPredictionSpecificMeasurement(
            @PathVariable String measurement) {
        return insightClient.getPredictionSpecificMeasurement(measurement);
    }

    @GetMapping("/predictions/options")
    public ResBody<PredictionOptions> getPredictionOptions() {
        return insightClient.getPredictionOptions();
    }

    @PostMapping("/predictions/nsId/{nsId}/target/{targetId}")
    public ResBody<PredictionResult> predictMonitoringData(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestBody PredictionBody body) {
        return insightClient.predictMonitoringData(nsId, targetId, body);
    }

    @GetMapping("/predictions/nsId/{nsId}/target/{targetId}/history")
    public ResBody<PredictionHistory> getPredictionHistory(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightClient.getPredictionHistory(nsId, targetId, measurement, startTime, endTime);
    }
}
