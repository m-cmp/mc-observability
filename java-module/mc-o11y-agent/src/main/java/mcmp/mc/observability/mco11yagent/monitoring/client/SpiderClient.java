package mcmp.mc.observability.mco11yagent.monitoring.client;

import mcmp.mc.observability.mco11yagent.monitoring.config.SpiderFeignConfig;
import mcmp.mc.observability.mco11yagent.monitoring.model.SpiderMonitoringInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cb-spider", url = "${feign.cb-spider.url:}", configuration = SpiderFeignConfig.class)
public interface SpiderClient {
    @GetMapping(value = "/spider/monitoring/vm/{vmName}/{metricType}", produces = "application/json")
    SpiderMonitoringInfo.Data getVMMonitoring(@PathVariable String vmName, @PathVariable String metricType,
                                                    @RequestParam("ConnectionName") String connectionName,
                                                    @RequestParam("TimeBeforeHour") String timeBeforeHour,
                                                    @RequestParam("IntervalMinute") String intervalMinute);
}
