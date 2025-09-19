spring rest docs로 swaager ui띄울거야. 

insightclient, insightclientadatper, insightcontroller와 이미 만들어둔 itemController를 참고해서 
insightControllerTest에 추가해야할 부분이나 수정할 부분을 만들어줘. 

uitl클래스도 참고하고. 

참고로 insightController에 명시된대로 
TAG값은
[Insight] LLM

    // LLM API endpoints
    @GetMapping("/llm/model")
    public ResBodyLLMModel getLLMModelOptions() {
        return insightClient.getLLMModelOptions();
    }

    @GetMapping("/llm/session")
    public ResBodyLLMChatSessions getLLMChatSessions() {
        return insightClient.getLLMChatSessions();
    }

    @PostMapping("/llm/session")
    public ResBodyLLMChatSession postLLMChatSession(@RequestBody PostSessionBody body) {
        return insightClient.postLLMChatSession(body);
    }

    @DeleteMapping("/llm/session")
    public ResBodyLLMChatSession deleteLLMChatSession(@RequestBody SessionIdPath path) {
        return insightClient.deleteLLMChatSession(path);
    }

    @DeleteMapping("/llm/sessions")
    public ResBodyLLMChatSessions deleteAllLLMChatSessions() {
        return insightClient.deleteAllLLMChatSessions();
    }

    @GetMapping("/llm/session/{sessionId}/history")
    public ResBodySessionHistory getLLMSessionHistory(@PathVariable("sessionId") String sessionId) {
        return insightClient.getLLMSessionHistory(sessionId);
    }


[Insight] Log Analysis

    // Log/Alert Analysis endpoints
    @PostMapping("/log-analysis/query")
    public ResBodyQuery postLogAnalysisQuery(@RequestBody PostQueryBody body) {
        return insightClient.postLogAnalysisQuery(body);
    }

[Insight] Alert Analysis
@PostMapping("/alert-analysis/query")
public ResBodyQuery postAlertAnalysisQuery(@RequestBody PostQueryBody body) {
return insightClient.postAlertAnalysisQuery(body);
}

[Insight] anomaly-detection
// insight anomaly detection
@GetMapping("/insight/anomaly-detection" + "/measurement")
public Object getAnomalyDetectionMeasurement() {
return insightClient.getAnomalyDetectionMeasurement();
}

    @GetMapping("/insight/anomaly-detection" + "/measurement/{measurement}")
    public Object getAnomalyDetectionSpecificMeasurement(@PathVariable String measurement) {
        return insightClient.getAnomalyDetectionSpecificMeasurement(measurement);
    }

    @GetMapping("/insight/anomaly-detection" + "/options")
    public Object getAnomalyDetectionOptions() {
        return insightClient.getAnomalyDetectionOptions();
    }

    @GetMapping("/insight/anomaly-detection" + "/settings")
    public Object getAnomalyDetectionSettings() {
        return insightClient.getAnomalyDetectionSettings();
    }

    @PostMapping("/insight/anomaly-detection" + "/settings")
    public Object insertAnomalyDetectionSetting(@RequestBody Object body) {
        return insightClient.insertAnomalyDetectionSetting(body);
    }

    @PutMapping("/insight/anomaly-detection" + "/settings/{settingSeq}")
    public Object updateAnomalyDetectionSetting(
            @PathVariable Long settingSeq, @RequestBody Object body) {
        return insightClient.updateAnomalyDetectionSetting(settingSeq, body);
    }

    @DeleteMapping("/insight/anomaly-detection" + "/settings/{settingSeq}")
    public Object deleteAnomalyDetectionSetting(@PathVariable Long settingSeq) {
        return insightClient.deleteAnomalyDetectionSetting(settingSeq);
    }

    @GetMapping("/insight/anomaly-detection" + "/settings/nsId/{nsId}/vm/{vmId}")
    public Object getAnomalyDetection(@PathVariable String nsId, @PathVariable String vmId) {
        return insightClient.getAnomalyDetection(nsId, vmId);
    }

    @GetMapping("/insight/anomaly-detection" + "/nsId/{nsId}/vm/{vmId}/history")
    public Object getAnomalyDetectionHistory(
            @PathVariable String nsId,
            @PathVariable String vmId,
            @RequestParam String measurement,
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time) {
        return insightClient.getAnomalyDetectionHistory(
                nsId, vmId, measurement, start_time, end_time);
    }
[Insight] Prediction

    // insight prediction
    @GetMapping("/insight/predictions" + "/measurement")
    public Object getPredictionMeasurement() {
        return insightClient.getPredictionMeasurement();
    }

    @GetMapping("/insight/predictions" + "/measurement/{measurement}")
    public Object getPredictionSpecificMeasurement(@PathVariable String measurement) {
        return insightClient.getPredictionSpecificMeasurement(measurement);
    }

    @GetMapping("/insight/predictions" + "/options")
    public Object getPredictionOptions() {
        return insightClient.getPredictionOptions();
    }

    @PostMapping("/insight/predictions" + "/nsId/{nsId}/vm/{vmId}")
    public Object predictMetric(
            @PathVariable String nsId, @PathVariable String vmId, @RequestBody Object body) {
        return insightClient.predictMetric(nsId, vmId, body);
    }

    @GetMapping("/insight/predictions" + "/nsId/{nsId}/vm/{vmId}/history")
    public Object getPredictionHistory(
            @PathVariable String nsId,
            @PathVariable String vmId,
            @RequestParam String measurement,
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time) {
        return insightClient.getPredictionHistory(nsId, vmId, measurement, start_time, end_time);
    }

야 그리고 각각의 example들 값은 이거 니가 다 이해하고 네라고 대답하면 알려줄게 .




