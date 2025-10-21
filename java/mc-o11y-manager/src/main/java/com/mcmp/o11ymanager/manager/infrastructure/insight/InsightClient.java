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

    @GetMapping(ANOMALY + "/settings/ns/{nsId}/mci/{mciId}")
    Object getAnomalySettingsForMci(
            @PathVariable("nsId") String nsId, @PathVariable("mciId") String mciId);

    @GetMapping(ANOMALY + "/settings/ns/{nsId}/mci/{mciId}/vm/{vmId}")
    Object getAnomalySettingsForVm(
            @PathVariable("nsId") String nsId,
            @PathVariable("mciId") String mciId,
            @PathVariable("vmId") String vmId);

    @GetMapping(ANOMALY + "/ns/{nsId}/mci/{mciId}/history")
    Object getAnomalyHistoryForMci(
            @PathVariable("nsId") String nsId,
            @PathVariable("mciId") String mciId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);

    @GetMapping(ANOMALY + "/ns/{nsId}/mci/{mciId}/vm/{vmId}/history")
    Object getAnomalyHistoryForVm(
            @PathVariable("nsId") String nsId,
            @PathVariable("mciId") String mciId,
            @PathVariable("vmId") String vmId,
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

    /** POST /predictions/ns/{nsId}/mci/{mciId} */
    @PostMapping(PREDICTION + "/ns/{nsId}/mci/{mciId}")
    Object predictMonitoringDataForMci(
            @PathVariable("nsId") String nsId,
            @PathVariable("mciId") String mciId,
            @RequestBody Object body);

    /** POST /predictions/ns/{nsId}/mci/{mciId}/vm/{vmId} */
    @PostMapping(PREDICTION + "/ns/{nsId}/mci/{mciId}/vm/{vmId}")
    Object predictMonitoringDataForVm(
            @PathVariable("nsId") String nsId,
            @PathVariable("mciId") String mciId,
            @PathVariable("vmId") String vmId,
            @RequestBody Object body);

    /** GET /predictions/ns/{nsId}/mci/{mciId}/history */
    @GetMapping(PREDICTION + "/ns/{nsId}/mci/{mciId}/history")
    Object getPredictionHistoryForMci(
            @PathVariable("nsId") String nsId,
            @PathVariable("mciId") String mciId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);

    /** GET /predictions/ns/{nsId}/mci/{mciId}/vm/{vmId}/history */
    @GetMapping(PREDICTION + "/ns/{nsId}/mci/{mciId}/vm/{vmId}/history")
    Object getPredictionHistoryForVm(
            @PathVariable("nsId") String nsId,
            @PathVariable("mciId") String mciId,
            @PathVariable("vmId") String vmId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime);
}
