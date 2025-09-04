package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.infrastructure.insight.InsightClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/insight")
public class InsightController {

    private final InsightClient insightClient;

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

    @PostMapping("/insight/predictions" + "/nsId/{nsId}/target/{targetId}")
    public Object predictMetric(
            @PathVariable String nsId, @PathVariable String targetId, @RequestBody Object body) {
        return insightClient.predictMetric(nsId, targetId, body);
    }

    @GetMapping("/insight/predictions" + "/nsId/{nsId}/target/{targetId}/history")
    public Object getPredictionHistory(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestParam String measurement,
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time) {
        return insightClient.getPredictionHistory(
                nsId, targetId, measurement, start_time, end_time);
    }

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

    @GetMapping("/insight/anomaly-detection" + "/settings/nsId/{nsId}/target/{targetId}")
    public Object getAnomalyDetection(@PathVariable String nsId, @PathVariable String targetId) {
        return insightClient.getAnomalyDetection(nsId, targetId);
    }

    @GetMapping("/insight/anomaly-detection" + "/nsId/{nsId}/target/{targetId}/history")
    public Object getAnomalyDetectionHistory(
            @PathVariable String nsId,
            @PathVariable String targetId,
            @RequestParam String measurement,
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time) {
        return insightClient.getAnomalyDetectionHistory(
                nsId, targetId, measurement, start_time, end_time);
    }
}
