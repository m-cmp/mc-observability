package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.SpiderClusterInfo;
import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.infrastructure.spider.SpiderClient;
import com.mcmp.o11ymanager.manager.service.cache.ClusterListCacheService;
import com.mcmp.o11ymanager.manager.service.cache.CspCacheKey;
import com.mcmp.o11ymanager.manager.service.cache.CspCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cached proxy for cb-spider monitoring responses.
 *
 * <p>Returns the raw cb-spider payload (no {@code ResBody} wrapping) so existing clients that
 * talked directly to {@code /spider/monitoring/**} only need to repoint the base path.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/csp")
@Tag(name = "[Manager] CSP Monitoring (Cached)")
public class CspMonitoringController {

    private final SpiderClient spiderClient;
    private final CspCacheService cspCacheService;
    private final ClusterListCacheService clusterListCacheService;

    @GetMapping("/clusters")
    @Operation(
            summary = "GetK8sClusters (cached)",
            description =
                    "List K8s clusters (with node-group detail) for a connection, cached ~30s to"
                            + " avoid hitting cb-spider on every page load.")
    public List<SpiderClusterInfo> getClusters(
            @RequestParam("ConnectionName") String connectionName) {
        return clusterListCacheService.get(connectionName);
    }

    @GetMapping("/node/{nodeName}/{measurement}")
    @Operation(
            summary = "GetVMMonitoring",
            operationId = "GetVMMonitoringCached",
            description =
                    "Proxy cb-spider /spider/monitoring/vm/{vmName}/{measurement} with Caffeine caching.")
    public SpiderMonitoringInfo.Data getVmMetric(
            @Parameter(description = "CSP resource name of the VM") @PathVariable String nodeName,
            @Parameter(description = "Metric type (e.g. cpu_usage, memory_usage)") @PathVariable
                    String measurement,
            @RequestParam("ConnectionName") String connectionName,
            @RequestParam(value = "TimeBeforeHour", required = false) String timeBeforeHour,
            @RequestParam(value = "timeBeforeHour", required = false) String timeBeforeHourAlt,
            @RequestParam(value = "IntervalMinute", required = false) String intervalMinute,
            @RequestParam(value = "periodMinute", required = false) String periodMinute) {
        String tbh = firstNonBlank(timeBeforeHour, timeBeforeHourAlt, "1");
        String ivm = firstNonBlank(intervalMinute, periodMinute, "5");
        CspCacheKey key = CspCacheKey.forVm(nodeName, measurement, connectionName, tbh, ivm);
        return cspCacheService.getOrLoad(
                key,
                () -> {
                    try {
                        return spiderClient.getVMMonitoring(
                                nodeName, measurement, connectionName, tbh, ivm);
                    } catch (Exception e) {
                        // cb-spider has no CloudWatch monitoring for this resource (e.g. a k8s node
                        // queried as a VM, or an unsupported metric). Return empty so the UI shows
                        // "no data" instead of a 500.
                        log.debug(
                                "cb-spider VM monitoring unavailable conn={} node={} metric={}: {}",
                                connectionName,
                                nodeName,
                                measurement,
                                e.toString());
                        return emptyData(measurement);
                    }
                });
    }

    @GetMapping("/cluster/{clusterName}/{nodeGroupName}/{nodeNumber}/{measurement}")
    @Operation(
            summary = "GetClusterNodeMonitoring",
            operationId = "GetClusterNodeMonitoringCached",
            description =
                    "Proxy cb-spider /spider/monitoring/clusternode/{...} with Caffeine caching.")
    public SpiderMonitoringInfo.Data getClusterNodeMetric(
            @PathVariable String clusterName,
            @PathVariable String nodeGroupName,
            @PathVariable String nodeNumber,
            @PathVariable String measurement,
            @RequestParam("ConnectionName") String connectionName,
            @RequestParam(value = "TimeBeforeHour", required = false) String timeBeforeHour,
            @RequestParam(value = "timeBeforeHour", required = false) String timeBeforeHourAlt,
            @RequestParam(value = "IntervalMinute", required = false) String intervalMinute,
            @RequestParam(value = "periodMinute", required = false) String periodMinute) {
        String tbh = firstNonBlank(timeBeforeHour, timeBeforeHourAlt, "1");
        String ivm = firstNonBlank(intervalMinute, periodMinute, "5");
        CspCacheKey key =
                CspCacheKey.forClusterNode(
                        clusterName,
                        nodeGroupName,
                        nodeNumber,
                        measurement,
                        connectionName,
                        tbh,
                        ivm);
        return cspCacheService.getOrLoad(
                key,
                () -> {
                    try {
                        return spiderClient.getClusterNodeMonitoring(
                                clusterName,
                                nodeGroupName,
                                nodeNumber,
                                measurement,
                                connectionName,
                                tbh,
                                ivm);
                    } catch (Exception e) {
                        log.debug(
                                "cb-spider cluster-node monitoring unavailable conn={} cluster={}"
                                        + " node={} metric={}: {}",
                                connectionName,
                                clusterName,
                                nodeNumber,
                                measurement,
                                e.toString());
                        return emptyData(measurement);
                    }
                });
    }

    /**
     * An empty series flagged unsupported, used when cb-spider can't monitor the resource/metric.
     */
    private static SpiderMonitoringInfo.Data emptyData(String measurement) {
        SpiderMonitoringInfo.Data d = new SpiderMonitoringInfo.Data();
        d.setMetricName(measurement);
        d.setTimestampValues(java.util.List.of());
        d.setUnsupported(true);
        return d;
    }

    @GetMapping("/cache/stats")
    @Operation(summary = "GetCspCacheStats")
    public ResBody<Map<String, Object>> cacheStats() {
        return new ResBody<>(cspCacheService.stats());
    }

    @DeleteMapping("/cache")
    @Operation(summary = "InvalidateCspCache")
    public ResBody<String> invalidateCache() {
        cspCacheService.invalidateAll();
        return new ResBody<>("ok");
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
