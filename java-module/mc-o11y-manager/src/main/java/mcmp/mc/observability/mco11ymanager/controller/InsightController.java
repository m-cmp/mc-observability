package mcmp.mc.observability.mco11ymanager.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11ymanager.client.InsightClient;
import mcmp.mc.observability.mco11ymanager.common.Constants;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.PREFIX_V1)
public class InsightController {
    private final InsightClient insightClient;

    // insight prediction
    @GetMapping(Constants.PREDICTION_PATH + "/options")
    public Object getPredictionOptions() {
        return insightClient.getPredictionOptions();
    }
    @PostMapping(Constants.PREDICTION_PATH + "/nsId/{nsId}/target/{targetId}")
    public Object predictMetric(@PathVariable String nsId, @PathVariable String targetId, @RequestBody Object body) {
        return insightClient.predictMetric(nsId, targetId, body);
    }
    @GetMapping(Constants.PREDICTION_PATH + "/nsId/{nsId}/target/{targetId}/history")
    public Object getPredictionHistory(@PathVariable String nsId, @PathVariable String targetId, @RequestParam String measurement, @RequestParam(required = false) String start_time, @RequestParam(required = false) String end_time) {
        return insightClient.getPredictionHistory(nsId, targetId, measurement, start_time, end_time);
    }

    // insight anomaly detection
    @GetMapping(Constants.ANOMALY_PATH + "/options")
    public Object getAnomalyDetectionOptions() {
        return insightClient.getAnomalyDetectionOptions();
    }
    @GetMapping(Constants.ANOMALY_PATH + "/settings")
    public Object getAnomalyDetectionSettings() {
        return insightClient.getAnomalyDetectionSettings();
    }
    @PostMapping(Constants.ANOMALY_PATH + "/settings")
    public Object insertAnomalyDetectionSetting(@RequestBody Object body) {
        return insightClient.insertAnomalyDetectionSetting(body);
    }
    @PutMapping(Constants.ANOMALY_PATH + "/settings/{settingSeq}")
    public Object updateAnomalyDetectionSetting(@RequestParam Long settingSeq, @RequestBody Object body) {
        return insightClient.updateAnomalyDetectionSetting(settingSeq, body);
    }
    @DeleteMapping(Constants.ANOMALY_PATH + "/settings/{settingSeq}")
    public Object deleteAnomalyDetectionSetting(@RequestParam Long settingSeq) {
        return insightClient.deleteAnomalyDetectionSetting(settingSeq);
    }
    @GetMapping(Constants.ANOMALY_PATH + "/settings/nsId/{nsId}/target/{targetId}")
    public Object getAnomalyDetection(@PathVariable String nsId, @PathVariable String targetId) {
        return insightClient.getAnomalyDetection(nsId, targetId);
    }
    @GetMapping(Constants.ANOMALY_PATH + "/nsId/{nsId}/target/{targetId}/history")
    public Object getAnomalyDetectionHistory(@PathVariable String nsId, @PathVariable String targetId, @RequestParam String measurement, @RequestParam String start_time, @RequestParam String end_time) {
        return insightClient.getAnomalyDetectionHistory(nsId, targetId, measurement, start_time, end_time);
    }
}
