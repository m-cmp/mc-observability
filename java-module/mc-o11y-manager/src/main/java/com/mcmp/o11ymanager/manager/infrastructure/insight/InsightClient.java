package com.mcmp.o11ymanager.manager.infrastructure.insight;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "insight", url = "${feign.insight.url}")
public interface InsightClient {

    String ANOMALY = "/anomaly-detection";
    String ALERT = "/alert-analysis";
    String LLM = "/llm";
    String LOG = "/log-analysis";
    String PREDICTION = "/predictions";

    @GetMapping(ANOMALY + "/measurement")
    Object getMeasurements();

    @GetMapping(ANOMALY + "/measurement/{measurement}")
    Object getSpecificMeasurement(
            @PathVariable("measurement") String measurement);

    @GetMapping(ANOMALY + "/options")
    Object getOptions();

    @PostMapping(ANOMALY + "/nsId/{nsId}/target/{targetId}")
    Object predictMetric(
            @PathVariable("nsId") String nsId,
            @PathVariable("targetId") String targetId,
            @RequestBody Object body);

    @GetMapping(ANOMALY + "/nsId/{nsId}/target/{targetId}/history")
    Object getAnomalyHistory(
            @PathVariable("nsId") String nsId,
            @PathVariable("targetId") String targetId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);

    @PostMapping(ALERT + "/query")
    Object queryAlertAnalysis(@RequestBody Object body);

    @GetMapping(LLM + "/model")
    Object getLLMModelOptions();

    @GetMapping(LLM + "/session")
    Object getLLMChatSessions();

    @PostMapping(LLM + "/session")
    Object postLLMChatSession(@RequestBody Object body);

    @DeleteMapping(LLM + "/session")
    Object deleteLLMChatSession(@RequestParam("sessionId") String sessionId);

    @DeleteMapping(LLM + "/sessions")
    Object deleteAllLLMChatSessions();

    @GetMapping(LLM + "/session/{sessionId}/history")
    Object getLLMSessionHistory();

    @PostMapping(LOG + "/query")
    Object queryLogAnalysis();

    @GetMapping(PREDICTION + "/measurement")
    Object getPredictionMeasurements();

    /** GET /predictions/measurement/{measurement} */
    @GetMapping(PREDICTION + "/measurement/{measurement}")
    Object getPredictionSpecificMeasurement(
            @PathVariable("measurement") String measurement);

    /** GET /predictions/options */
    @GetMapping(PREDICTION + "/options")
    Object getPredictionOptions();

    /** POST /predictions/nsId/{nsId}/vm/{targetId} */
    @PostMapping(PREDICTION + "/nsId/{nsId}/target/{targetId}")
    Object predictMonitoringData(
            @RequestBody Object body);

    /** GET /predictions/nsId/{nsId}/vm/{targetId}/history */
    @GetMapping(PREDICTION + "/nsId/{nsId}/target/{targetId}/history")
    Object getPredictionHistory(
            @PathVariable("nsId") String nsId,
            @PathVariable("targetId") String targetId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);
}
