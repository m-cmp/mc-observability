package com.mcmp.o11ymanager.manager.infrastructure.insight;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "insight", url = "${feign.insight.url}")
public interface InsightClient {

    String ANOMALY = "/api/o11y/insight/anomaly-detection";
    String ALERT = "/api/o11y/insight/alert-analysis";
    String LLM = "/api/o11y/insight/llm";
    String LOG = "/api/o11y/insight/log-analysis";
    String PREDICTION = "/api/o11y/insight/predictions";
    String SERVER_ERROR = "/api/o11y/insight/server-error-analysis";

    @GetMapping(ANOMALY + "/measurement")
    Object getMeasurements();

    @GetMapping(ANOMALY + "/measurement/{measurement}")
    Object getSpecificMeasurement(@PathVariable("measurement") String measurement);

    @GetMapping(ANOMALY + "/options")
    Object getOptions();

    @PostMapping(ANOMALY + "/{settingSeq}")
    Object predictMetric(@PathVariable("settingSeq") int settingSeq);

    @GetMapping(ANOMALY + "/settings")
    Object getAnomalySettings();

    @PostMapping(ANOMALY + "/settings")
    Object createAnomalySetting(@RequestBody Object body);

    @PutMapping(ANOMALY + "/settings/{settingSeq}")
    Object updateAnomalySetting(
            @PathVariable("settingSeq") int settingSeq, @RequestBody Object body);

    @DeleteMapping(ANOMALY + "/settings/{settingSeq}")
    Object deleteAnomalySetting(@PathVariable("settingSeq") int settingSeq);

    @GetMapping(ANOMALY + "/settings/ns/{nsId}/infra/{infraId}")
    Object getAnomalySettingsForInfra(
            @PathVariable("nsId") String nsId, @PathVariable("infraId") String infraId);

    @GetMapping(ANOMALY + "/settings/ns/{nsId}/infra/{infraId}/node/{nodeId}")
    Object getAnomalySettingsForNode(
            @PathVariable("nsId") String nsId,
            @PathVariable("infraId") String infraId,
            @PathVariable("nodeId") String nodeId);

    @GetMapping(ANOMALY + "/ns/{nsId}/infra/{infraId}/history")
    Object getAnomalyHistoryForInfra(
            @PathVariable("nsId") String nsId,
            @PathVariable("infraId") String infraId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);

    @GetMapping(ANOMALY + "/ns/{nsId}/infra/{infraId}/node/{nodeId}/history")
    Object getAnomalyHistoryForNode(
            @PathVariable("nsId") String nsId,
            @PathVariable("infraId") String infraId,
            @PathVariable("nodeId") String nodeId,
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
    Object getLLMSessionHistory(@PathVariable("sessionId") String sessionId);

    @GetMapping(LLM + "/api-keys")
    Object getLLMApiKeys(@RequestParam("provider") String provider);

    @PostMapping(LLM + "/api-keys")
    Object postLLMApiKeys(@RequestBody Object body);

    @DeleteMapping(LLM + "/api-keys")
    Object deleteLLMApiKey(@RequestParam("provider") String provider);

    @PostMapping(LOG + "/query")
    Object queryLogAnalysis(@RequestBody Object body);

    @GetMapping(PREDICTION + "/measurement")
    Object getPredictionMeasurements();

    /** GET /predictions/measurement/{measurement} */
    @GetMapping(PREDICTION + "/measurement/{measurement}")
    Object getPredictionSpecificMeasurement(@PathVariable("measurement") String measurement);

    /** GET /predictions/options */
    @GetMapping(PREDICTION + "/options")
    Object getPredictionOptions();

    /** POST /predictions/ns/{nsId}/infra/{infraId} */
    @PostMapping(PREDICTION + "/ns/{nsId}/infra/{infraId}")
    Object predictMonitoringDataForInfra(
            @PathVariable("nsId") String nsId,
            @PathVariable("infraId") String infraId,
            @RequestBody Object body);

    /** POST /predictions/ns/{nsId}/infra/{infraId}/node/{nodeId} */
    @PostMapping(PREDICTION + "/ns/{nsId}/infra/{infraId}/node/{nodeId}")
    Object predictMonitoringDataForNode(
            @PathVariable("nsId") String nsId,
            @PathVariable("infraId") String infraId,
            @PathVariable("nodeId") String nodeId,
            @RequestBody Object body);

    /** GET /predictions/ns/{nsId}/infra/{infraId}/history */
    @GetMapping(PREDICTION + "/ns/{nsId}/infra/{infraId}/history")
    Object getPredictionHistoryForInfra(
            @PathVariable("nsId") String nsId,
            @PathVariable("infraId") String infraId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);

    /** GET /predictions/ns/{nsId}/infra/{infraId}/node/{nodeId}/history */
    @GetMapping(PREDICTION + "/ns/{nsId}/infra/{infraId}/node/{nodeId}/history")
    Object getPredictionHistoryForNode(
            @PathVariable("nsId") String nsId,
            @PathVariable("infraId") String infraId,
            @PathVariable("nodeId") String nodeId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);

    /* ===================== Server Error Analysis ===================== */
    @PostMapping(SERVER_ERROR + "/detect")
    Object detectServerError(@RequestBody Object body);

    @PostMapping(SERVER_ERROR + "/query")
    Object queryServerError(@RequestBody Object body);

    @GetMapping(SERVER_ERROR + "/records")
    Object listServerErrorRecords(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) String fromDt,
            @RequestParam(value = "to", required = false) String toDt,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size);

    @GetMapping(SERVER_ERROR + "/records/{analysisId}")
    Object getServerErrorRecord(@PathVariable("analysisId") int analysisId);

    @PostMapping(SERVER_ERROR + "/records/{analysisId}/rerun")
    Object rerunServerErrorAnalysis(@PathVariable("analysisId") int analysisId);
}
