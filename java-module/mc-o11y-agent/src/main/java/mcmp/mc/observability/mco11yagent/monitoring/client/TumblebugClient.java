package mcmp.mc.observability.mco11yagent.monitoring.client;

import mcmp.mc.observability.mco11yagent.monitoring.config.TumblebugFeignConfig;
import mcmp.mc.observability.mco11yagent.monitoring.model.TumblebugMCI;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cb-tumblebug", url = "${feign.cb-tumblebug.url:}", configuration = TumblebugFeignConfig.class)
public interface TumblebugClient {
    @GetMapping(value = "/tumblebug/ns/{nsId}/mci/{mciId}/vm/{vmId}", produces = "application/json")
    TumblebugMCI.Vm getVM(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String vmId);
}
