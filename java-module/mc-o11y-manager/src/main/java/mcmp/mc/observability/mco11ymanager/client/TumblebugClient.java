package mcmp.mc.observability.mco11ymanager.client;

import mcmp.mc.observability.mco11ymanager.config.TumblebugFeignConfig;
import mcmp.mc.observability.mco11ymanager.model.TumblebugCmd;
import mcmp.mc.observability.mco11ymanager.model.TumblebugMCI;
import mcmp.mc.observability.mco11ymanager.model.TumblebugNS;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cb-tumblebug", url = "${feign.cb-tumblebug.url:}", configuration = TumblebugFeignConfig.class)
public interface TumblebugClient {
    @GetMapping(value = "/tumblebug/ns", produces = "application/json")
    TumblebugNS getNSList();
    @GetMapping(value = "/tumblebug/ns/{nsId}/mci/{mciId}", produces = "application/json")
    TumblebugMCI getMCIList(@PathVariable String nsId, @PathVariable String mciId);
    @PostMapping(value = "/ns/{nsId}/cmd/mci/{mciId}", produces = "application/json")
    Object sendCommand(@PathVariable String nsId, @PathVariable String mciId, @RequestParam String subGroupId, @RequestParam String vmId, @RequestBody TumblebugCmd tumblebugCmd);
}
