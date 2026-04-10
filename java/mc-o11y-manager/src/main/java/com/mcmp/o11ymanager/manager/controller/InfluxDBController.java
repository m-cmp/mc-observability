package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.manager.facade.InfluxDbFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.service.cache.MonitoringCacheService;
import com.mcmp.o11ymanager.manager.service.cache.MonitoringCacheWarmScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/influxdb")
@Tag(name = "[Manager] Monitoring Metric")
public class InfluxDBController {

    private final InfluxDbFacadeService influxDbFacadeService;
    private final MonitoringCacheService monitoringCacheService;
    private final org.springframework.beans.factory.ObjectProvider<MonitoringCacheWarmScheduler>
            monitoringCacheWarmScheduler;

    @GetMapping
    @Operation(
            summary = "GetAllInfluxDB",
            operationId = "GetAllInfluxDB",
            description = "Retrieve all InfluxDB servers")
    public ResBody<List<InfluxDTO>> getAllInfluxDB() {
        return new ResBody<>(influxDbFacadeService.getInfluxDbs());
    }

    @GetMapping("/measurement")
    @Operation(
            summary = "GetMeasurementFields",
            operationId = "GetMeasurementFields",
            description = "Retrieve InfluxDB measurements")
    public ResBody<List<FieldDTO>> measurement() {
        return new ResBody<>(influxDbFacadeService.getFields());
    }

    @GetMapping("/tag")
    @Operation(
            summary = "GetMeasurementTags",
            operationId = "GetMeasurementTags",
            description = "Retrieve InfluxDB tags")
    public ResBody<List<TagDTO>> tag() {
        return new ResBody<>(influxDbFacadeService.getTags());
    }

    @PostMapping("/metric/{nsId}/{mciId}")
    @Operation(
            summary = "GetMetricsByNsIdAndMciId",
            operationId = "GetMetricsByNsIdAndMciId",
            description = "Retrieve InfluxDB metrics")
    public ResBody<List<MetricDTO>> MetricByNsIdAndMciId(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @RequestBody MetricRequestDTO req) {
        return new ResBody<>(influxDbFacadeService.postMetricsByNsMci(nsId, mciId, req));
    }

    @PostMapping("/metric/{nsId}/{mciId}/{vmId}")
    @Operation(
            summary = "GetMetricsByVMId",
            operationId = "GetMetricsByVMId",
            description = "Retrieve InfluxDB metrics")
    public ResBody<List<MetricDTO>> MetricByVMId(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId,
            @RequestBody MetricRequestDTO req) {
        return new ResBody<>(influxDbFacadeService.postMetricsByVM(nsId, mciId, vmId, req));
    }

    @GetMapping("/cache/stats")
    @Operation(
            summary = "GetMonitoringCacheStats",
            operationId = "GetMonitoringCacheStats",
            description = "Retrieve in-memory monitoring metric cache statistics")
    public ResBody<Map<String, Object>> cacheStats() {
        return new ResBody<>(monitoringCacheService.stats());
    }

    @DeleteMapping("/cache")
    @Operation(
            summary = "InvalidateMonitoringCache",
            operationId = "InvalidateMonitoringCache",
            description = "Invalidate all entries in the monitoring metric cache")
    public ResBody<String> invalidateCache() {
        monitoringCacheService.invalidateAll();
        return new ResBody<>("ok");
    }

    @PostMapping("/cache/warm")
    @Operation(
            summary = "WarmMonitoringCache",
            operationId = "WarmMonitoringCache",
            description =
                    "Trigger an immediate cache-warming pass over the recently created VMs (top-N).")
    public ResBody<Map<String, Object>> warmCache() {
        MonitoringCacheWarmScheduler scheduler = monitoringCacheWarmScheduler.getIfAvailable();
        if (scheduler == null) {
            return new ResBody<>(Map.of("warmed", 0, "enabled", false));
        }
        int warmed = scheduler.warmNow();
        return new ResBody<>(Map.of("warmed", warmed, "enabled", true));
    }
}
