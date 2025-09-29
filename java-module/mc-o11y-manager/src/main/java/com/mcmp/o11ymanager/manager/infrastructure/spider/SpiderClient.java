package com.mcmp.o11ymanager.manager.infrastructure.spider;

import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;
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
}
