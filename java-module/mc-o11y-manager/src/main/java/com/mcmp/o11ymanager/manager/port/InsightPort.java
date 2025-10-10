package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.*;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.*;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.*;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;;

public interface InsightPort {

  /* ===================== Prediction ===================== */
  Object getPredictionMeasurements();

  Object getPredictionSpecificMeasurement(String measurement);

  Object getPredictionOptions();

  Object predictMonitoringData(String nsId, String vmId, Object body);

  Object getPredictionHistory(
      String nsId, String vmId, String measurement, String startTime, String endTime);

  /* ===================== Anomaly Detection ===================== */
  Object getMeasurements();

  Object getSpecificMeasurement(String measurement);

  Object getOptions();

  Object predictMetric(String nsId, String targetId, Object body);

  Object getAnomalyHistory(
      String nsId, String targetId, String measurement, String startTime, String endTime);

  /* ===================== LLM ===================== */
  Object getLLMModelOptions();

  Object getLLMChatSessions();

  Object postLLMChatSession(Object body);

  Object deleteLLMChatSession(String sessionId);

  Object deleteAllLLMChatSessions();

  Object getLLMSessionHistory(String sessionId);

  /* ===================== Alert Analysis ===================== */
  Object queryAlertAnalysis(Object body);

  /* ===================== Log Analysis ===================== */
  Object queryLogAnalysis(Object body);

}
