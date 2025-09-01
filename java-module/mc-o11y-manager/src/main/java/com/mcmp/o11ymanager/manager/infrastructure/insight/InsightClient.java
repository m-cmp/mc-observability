package com.mcmp.o11ymanager.manager.infrastructure.insight;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "insight", url = "${feign.insight.url:}")
public interface InsightClient {

  @GetMapping("/api/o11y" +  "/insight/predictions" + "/measurement")
  Object getPredictionMeasurement();
  @GetMapping("/api/o11y" +  "/insight/predictions" + "/measurement/{measurement}")
  Object getPredictionSpecificMeasurement(@PathVariable String measurement);
  @GetMapping("/api/o11y" +  "/insight/predictions" + "/options")
  Object getPredictionOptions();
  @PostMapping("/api/o11y" +  "/insight/predictions" + "/nsId/{nsId}/target/{targetId}")
  Object predictMetric(@PathVariable String nsId, @PathVariable String targetId, @RequestBody Object option);
  @GetMapping("/api/o11y" +  "/insight/predictions" + "/nsId/{nsId}/target/{targetId}/history")
  Object getPredictionHistory(@PathVariable String nsId, @PathVariable String targetId, @RequestParam String measurement, @RequestParam(required = false) String start_time, @RequestParam(required = false) String end_time);

  @GetMapping("/api/o11y" + "/insight/anomaly-detection" + "/measurement")
  Object getAnomalyDetectionMeasurement();
  @GetMapping("/api/o11y" + "/insight/anomaly-detection" + "/measurement/{measurement}")
  Object getAnomalyDetectionSpecificMeasurement(@PathVariable String measurement);
  @GetMapping("/api/o11y" + "/insight/anomaly-detection" + "/options")
  Object getAnomalyDetectionOptions();
  @GetMapping("/api/o11y" + "/insight/anomaly-detection" + "/settings")
  Object getAnomalyDetectionSettings();
  @PostMapping("/api/o11y" + "/insight/anomaly-detection" + "/settings")
  Object insertAnomalyDetectionSetting(@RequestBody Object body);
  @PutMapping("/api/o11y" + "/insight/anomaly-detection" + "/settings/{settingSeq}")
  Object updateAnomalyDetectionSetting(@PathVariable Long settingSeq, @RequestBody Object body);
  @DeleteMapping("/api/o11y" + "/insight/anomaly-detection" + "/settings/{settingSeq}")
  Object deleteAnomalyDetectionSetting(@PathVariable Long settingSeq);
  @GetMapping("/api/o11y" + "/insight/anomaly-detection" + "/settings/nsId/{nsId}/target/{targetId}")
  Object getAnomalyDetection(@PathVariable String nsId, @PathVariable String targetId);
  @GetMapping("/api/o11y" + "/insight/anomaly-detection" + "/nsId/{nsId}/target/{targetId}/history")
  Object getAnomalyDetectionHistory(@PathVariable String nsId, @PathVariable String targetId, @RequestParam String measurement, @RequestParam(required = false) String start_time, @RequestParam(required = false) String end_time);
}
