package com.mcmp.o11ymanager.manager.infrastructure.spider;

import com.mcmp.o11ymanager.manager.dto.SpiderClusterInfo;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterList;
import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
}
