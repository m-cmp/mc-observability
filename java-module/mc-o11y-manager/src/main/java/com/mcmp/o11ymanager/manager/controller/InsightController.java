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

  @PostMapping("/anomaly-detection/nsId/{nsId}/target/{targetId}")
  public Object predictMetric(
      @PathVariable String nsId,
      @PathVariable String targetId,
      @RequestBody Object body) {
    return insightPort.predictMetric(nsId, targetId, body);
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
  public Object queryAlertAnalysis(@RequestBody RequestBody body) {
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
  public Object postLLMChatSession(@RequestBody RequestBody body) {
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

  /* ===================== LOG ===================== */
  @PostMapping("/log-analysis/query")
  public Object queryLogAnalysis(@RequestBody RequestBody body) {
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
      @PathVariable String nsId,
      @PathVariable String targetId,
      @RequestBody Object body) {
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
