package com.mcmp.o11ymanager.manager.infrastructure.insight;

import com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection.*;
import com.mcmp.o11ymanager.manager.dto.insight.llm_analysis.*;
import com.mcmp.o11ymanager.manager.dto.insight.prediction.*;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "insight", url = "${feign.insight.url:}")
public interface InsightClient {

    String ANOMALY = "/anomaly-detection";
    String ALERT = "/alert-analysis";
    String LLM = "/llm";
    String LOG = "/log-analysis";
    String PREDICTION = "/predictions";

    @GetMapping(ANOMALY + "/measurement")
    ResBody<List<AnomalyDetectionMeasurement>> getMeasurements();

    @GetMapping(ANOMALY + "/measurement/{measurement}")
    ResBody<AnomalyDetectionMeasurement> getSpecificMeasurement(
            @PathVariable("measurement") String measurement);

    @GetMapping(ANOMALY + "/options")
    ResBody<AnomalyDetectionOptions> getOptions();

    @PostMapping(ANOMALY + "/nsId/{nsId}/target/{targetId}")
    ResBody<PredictionResult> predictMetric(
            @PathVariable("nsId") String nsId,
            @PathVariable("targetId") String targetId,
            @RequestBody PredictionBody body);

    @GetMapping(ANOMALY + "/nsId/{nsId}/target/{targetId}/history")
    ResBody<PredictionHistory> getAnomalyHistory(
            @PathVariable("nsId") String nsId,
            @PathVariable("targetId") String targetId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);

    @PostMapping(ALERT + "/query")
    ResBody<Message> queryAlertAnalysis(@RequestBody PostQueryBody body);

    @GetMapping(LLM + "/model")
    ResBody<List<LLMModel>> getLLMModelOptions();

    @GetMapping(LLM + "/session")
    ResBody<List<LLMChatSession>> getLLMChatSessions();

    @PostMapping(LLM + "/session")
    ResBody<LLMChatSession> postLLMChatSession(@RequestBody PostSessionBody body);

    @DeleteMapping(LLM + "/session")
    ResBody<LLMChatSession> deleteLLMChatSession(@RequestParam("sessionId") String sessionId);

    @DeleteMapping(LLM + "/sessions")
    ResBody<List<LLMChatSession>> deleteAllLLMChatSessions();

    @GetMapping(LLM + "/session/{sessionId}/history")
    ResBody<SessionHistory> getLLMSessionHistory(@PathVariable("sessionId") String sessionId);

    @PostMapping(LOG + "/query")
    ResBody<Message> queryLogAnalysis(@RequestBody PostQueryBody body);

    @GetMapping(PREDICTION + "/measurement")
    ResBody<List<PredictionMeasurement>> getPredictionMeasurements();

    /** GET /predictions/measurement/{measurement} */
    @GetMapping(PREDICTION + "/measurement/{measurement}")
    ResBody<PredictionMeasurement> getPredictionSpecificMeasurement(
            @PathVariable("measurement") String measurement);

    /** GET /predictions/options */
    @GetMapping(PREDICTION + "/options")
    ResBody<PredictionOptions> getPredictionOptions();

    /** POST /predictions/nsId/{nsId}/vm/{targetId} */
    @PostMapping(PREDICTION + "/nsId/{nsId}/target/{targetId}")
    ResBody<PredictionResult> predictMonitoringData(
            @PathVariable("nsId") String nsId,
            @PathVariable("targetId") String targetId,
            @RequestBody PredictionBody body);

    /** GET /predictions/nsId/{nsId}/vm/{targetId}/history */
    @GetMapping(PREDICTION + "/nsId/{nsId}/target/{targetId}/history")
    ResBody<PredictionHistory> getPredictionHistory(
            @PathVariable("nsId") String nsId,
            @PathVariable("targetId") String targetId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);
}
