package com.mcmp.o11ymanager.manager.infrastructure.spider;

import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;
import com.mcmp.o11ymanager.manager.dto.SpiderVm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "cb-spider",
        url = "${feign.cb-spider.url}",
        configuration = SpiderFeignConfig.class)
public interface SpiderClient {
    @GetMapping(
            value = "/spider/monitoring/vm/{vmName}/{measurement}",
            produces = "application/json")
    SpiderMonitoringInfo.Data getVMMonitoring(
            @PathVariable String vmName,
            @PathVariable String measurement,
            @RequestParam("ConnectionName") String connectionName,
            @RequestParam("TimeBeforeHour") String timeBeforeHour,
            @RequestParam("IntervalMinute") String intervalMinute);

    @GetMapping(
            value =
                    "/spider/monitoring/clusternode/{clusterName}/{nodeGroupName}/{nodeNumber}/{measurement}",
            produces = "application/json")
    SpiderMonitoringInfo.Data getClusterNodeMonitoring(
            @PathVariable String clusterName,
            @PathVariable String nodeGroupName,
            @PathVariable String nodeNumber,
            @PathVariable String measurement,
            @RequestParam("ConnectionName") String connectionName,
            @RequestParam("TimeBeforeHour") String timeBeforeHour,
            @RequestParam("IntervalMinute") String intervalMinute);

    @GetMapping(value = "/spider/cluster", produces = "application/json")
    SpiderClusterList listClusters(@RequestParam("ConnectionName") String connectionName);

    @GetMapping(value = "/spider/cluster/{clusterName}", produces = "application/json")
    SpiderClusterInfo getCluster(
            @PathVariable String clusterName,
            @RequestParam("ConnectionName") String connectionName);

    /**
     * Spider raw VM 조회. Tumblebug 응답의 publicIP가 빈 문자열로 오는 경우(OpenStack 등)에 fallback
     * 소스로 사용. {@code vmName}은 CSP의 cspResourceName 또는 Spider 측 NameId.
     */
    @GetMapping(value = "/spider/vm/{vmName}", produces = "application/json")
    SpiderVm getVm(
            @PathVariable String vmName, @RequestParam("ConnectionName") String connectionName);
}
