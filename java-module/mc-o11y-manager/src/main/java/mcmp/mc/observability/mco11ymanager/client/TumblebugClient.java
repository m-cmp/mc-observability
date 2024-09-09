package mcmp.mc.observability.mco11ymanager.client;

import mcmp.mc.observability.mco11ymanager.config.TumblebugFeignConfig;
import mcmp.mc.observability.mco11ymanager.model.TumblebugMCI;
import mcmp.mc.observability.mco11ymanager.model.TumblebugNS;
import mcmp.mc.observability.mco11ymanager.model.TumblebugSshKey;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cb-tumblebug", url = "${feign.cb-tumblebug.url:}", configuration = TumblebugFeignConfig.class)
public interface TumblebugClient {
    @GetMapping(value = "/tumblebug/ns", produces = "application/json")
    TumblebugNS getNSList();
    @GetMapping(value = "/tumblebug/ns/{nsId}/mci", produces = "application/json")
    TumblebugMCI getMCIList(@PathVariable String nsId);
    @GetMapping(value = "/tumblebug/ns/{nsId}/resources/sshkey", produces = "application/json")
    TumblebugSshKey getSshKey(@PathVariable String nsId, @RequestParam String id);
}
