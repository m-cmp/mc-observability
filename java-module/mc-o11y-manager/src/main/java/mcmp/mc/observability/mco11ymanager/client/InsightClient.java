package mcmp.mc.observability.mco11ymanager.client;

import mcmp.mc.observability.mco11ymanager.common.Constants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "insight", url = "${feign.insight.url:}")
public interface InsightClient {

    @GetMapping(Constants.PREFIX_V1 + Constants.PREDICTION_PATH + "/options")
    Object getPredictionOptions();
    @PostMapping(Constants.PREFIX_V1 + Constants.PREDICTION_PATH + "/nsId/{nsId}/target/{targetId}")
    Object predictMetric(@PathVariable String nsId, @PathVariable String targetId, @RequestBody Object option);
    @GetMapping(Constants.PREFIX_V1 + Constants.PREDICTION_PATH + "/nsId/{nsId}/target/{targetId}/history")
    Object getPredictionHistory(@PathVariable String nsId, @PathVariable String targetId, @RequestParam String measurement, @RequestParam(required = false) String start_time, @RequestParam(required = false) String end_time);

    @GetMapping(Constants.PREFIX_V1 + Constants.ANOMALY_PATH + "/options")
    Object getAnomalyDetectionOptions();
    @GetMapping(Constants.PREFIX_V1 + Constants.ANOMALY_PATH + "/settings")
    Object getAnomalyDetectionSettings();
    @PostMapping(Constants.PREFIX_V1 + Constants.ANOMALY_PATH + "/settings")
    Object insertAnomalyDetectionSetting(@RequestBody Object body);
    @PutMapping(Constants.PREFIX_V1 + Constants.ANOMALY_PATH + "/settings/{settingSeq}")
    Object updateAnomalyDetectionSetting(@PathVariable Long settingSeq, @RequestBody Object body);
    @DeleteMapping(Constants.PREFIX_V1 + Constants.ANOMALY_PATH + "/settings/{settingSeq}")
    Object deleteAnomalyDetectionSetting(@PathVariable Long settingSeq);
    @GetMapping(Constants.PREFIX_V1 + Constants.ANOMALY_PATH + "/settings/nsId/{nsId}/target/{targetId}")
    Object getAnomalyDetection(@PathVariable String nsId, @PathVariable String targetId);
    @GetMapping(Constants.PREFIX_V1 + Constants.ANOMALY_PATH + "/nsId/{nsId}/target/{targetId}/history")
    Object getAnomalyDetectionHistory(@PathVariable String nsId, @PathVariable String targetId, @RequestParam String measurement, @RequestParam(required = false) String start_time, @RequestParam(required = false) String end_time);
}
