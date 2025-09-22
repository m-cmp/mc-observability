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
import com.mcmp.o11ymanager.manager.port.InsightPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/insight")
public class InsightController {

    private final InsightPort insightPort;

    /* ===================== ANOMALY ===================== */
    @GetMapping("/anomaly-detection/measurement")
    public ResBody<List<AnomalyDetectionMeasurement>> getMeasurements() {
        return insightPort.getMeasurements();
    }

    @GetMapping("/anomaly-detection/measurement/{measurement}")
    public ResBody<AnomalyDetectionMeasurement> getSpecificMeasurement(
            @PathVariable String measurement) {
        return insightPort.getSpecificMeasurement(measurement);
    }

    @GetMapping("/anomaly-detection/options")
    public ResBody<AnomalyDetectionOptions> getOptions() {
        return insightPort.getOptions();
    }

    @PostMapping("/anomaly-detection/nsId/{nsId}/target/{targetId}")
    public ResBody<PredictionResult> predictMetric(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestBody PredictionBody body) {
        return insightPort.predictMetric(nsId, targetId, body);
    }

    @GetMapping("/anomaly-detection/nsId/{nsId}/target/{targetId}/history")
    public ResBody<PredictionHistory> getAnomalyHistory(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getAnomalyHistory(nsId, targetId, measurement, startTime, endTime);
    }

    /* ===================== ALERT ===================== */
    @PostMapping("/alert-analysis/query")
    public ResBody<Message> queryAlertAnalysis(@RequestBody PostQueryBody body) {
        return insightPort.queryAlertAnalysis(body);
    }

    /* ===================== LLM ===================== */
    @GetMapping("/llm/model")
    public ResBody<List<LLMModel>> getLLMModelOptions() {
        return insightPort.getLLMModelOptions();
    }

    @GetMapping("/llm/session")
    public ResBody<List<LLMChatSession>> getLLMChatSessions() {
        return insightPort.getLLMChatSessions();
    }

    @PostMapping("/llm/session")
    public ResBody<LLMChatSession> postLLMChatSession(@RequestBody PostSessionBody body) {
        return insightPort.postLLMChatSession(body);
    }

    @DeleteMapping("/llm/session")
    public ResBody<LLMChatSession> deleteLLMChatSession(@RequestParam String sessionId) {
        return insightPort.deleteLLMChatSession(sessionId);
    }

    @DeleteMapping("/llm/sessions")
    public ResBody<List<LLMChatSession>> deleteAllLLMChatSessions() {
        return insightPort.deleteAllLLMChatSessions();
    }

    @GetMapping("/llm/session/{sessionId}/history")
    public ResBody<SessionHistory> getLLMSessionHistory(@PathVariable String sessionId) {
        return insightPort.getLLMSessionHistory(sessionId);
    }

    /* ===================== LOG ===================== */
    @PostMapping("/log-analysis/query")
    public ResBody<Message> queryLogAnalysis(@RequestBody PostQueryBody body) {
        return insightPort.queryLogAnalysis(body);
    }

    /* ===================== PREDICTION ===================== */
    @GetMapping("/predictions/measurement")
    public ResBody<List<PredictionMeasurement>> getPredictionMeasurements() {
        return insightPort.getPredictionMeasurements();
    }

    @GetMapping("/predictions/measurement/{measurement}")
    public ResBody<PredictionMeasurement> getPredictionSpecificMeasurement(
            @PathVariable String measurement) {
        return insightPort.getPredictionSpecificMeasurement(measurement);
    }

    @GetMapping("/predictions/options")
    public ResBody<PredictionOptions> getPredictionOptions() {
        return insightPort.getPredictionOptions();
    }

    @PostMapping("/predictions/nsId/{nsId}/target/{targetId}")
    public ResBody<PredictionResult> predictMonitoringData(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestBody PredictionBody body) {
        return insightPort.predictMonitoringData(nsId, targetId, body); // ✅ 수정
    }

    @GetMapping("/predictions/nsId/{nsId}/target/{targetId}/history")
    public ResBody<PredictionHistory> getPredictionHistory(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestParam String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime) {
        return insightPort.getPredictionHistory(nsId, targetId, measurement, startTime, endTime);
    }
}
