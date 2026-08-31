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
    String RCA = "/api/o11y/insight/rca";

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

    @GetMapping(LLM + "/connections")
    Object getLLMConnections();

    @GetMapping(LLM + "/connections/{connectionId}")
    Object getLLMConnection(@PathVariable("connectionId") int connectionId);

    @PostMapping(LLM + "/connections")
    Object postLLMConnection(@RequestBody Object body);

    @PatchMapping(LLM + "/connections/{connectionId}")
    Object patchLLMConnection(
            @PathVariable("connectionId") int connectionId, @RequestBody Object body);

    @DeleteMapping(LLM + "/connections/{connectionId}")
    Object deleteLLMConnection(@PathVariable("connectionId") int connectionId);

    @GetMapping(LLM + "/connections/{connectionId}/models")
    Object getLLMConnectionModels(@PathVariable("connectionId") int connectionId);

    @PutMapping(LLM + "/connections/{connectionId}/default")
    Object setDefaultLLMConnection(
            @PathVariable("connectionId") int connectionId, @RequestBody Object body);

    @GetMapping(LLM + "/sessions")
    Object getLLMChatSessions();

    @PostMapping(LLM + "/sessions")
    Object postLLMChatSession(@RequestBody Object body);

    @DeleteMapping(LLM + "/sessions/{sessionId}")
    Object deleteLLMChatSession(@PathVariable("sessionId") String sessionId);

    @DeleteMapping(LLM + "/sessions")
    Object deleteAllLLMChatSessions();

    @GetMapping(LLM + "/sessions/{sessionId}/history")
    Object getLLMSessionHistory(@PathVariable("sessionId") String sessionId);

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

    /* ===================== RCA ===================== */
    @PostMapping(RCA + "/query")
    Object queryRca(@RequestBody Object body);

    @GetMapping(RCA + "/records")
    Object listRcaRecords(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) String fromDt,
            @RequestParam(value = "to", required = false) String toDt,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size);

    @GetMapping(RCA + "/records/{analysisId}")
    Object getRcaRecord(@PathVariable("analysisId") int analysisId);

    @GetMapping(RCA + "/schedules")
    Object listRcaSchedules();

    @PostMapping(RCA + "/schedules")
    Object postRcaSchedule(@RequestBody Object body);

    @PatchMapping(RCA + "/schedules/{scheduleId}")
    Object patchRcaSchedule(@PathVariable("scheduleId") int scheduleId, @RequestBody Object body);

    @DeleteMapping(RCA + "/schedules/{scheduleId}")
    Object deleteRcaSchedule(@PathVariable("scheduleId") int scheduleId);
}
