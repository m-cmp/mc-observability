package com.mcmp.o11ymanager.infrastructure.tumblebug;

import com.mcmp.o11ymanager.dto.tumblebug.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cb-tumblebug", url = "${feign.cb-tumblebug.url}", configuration = TumblebugFeignConfig.class)
public interface TumblebugClient {
  @GetMapping(value = "/tumblebug/ns/{nsId}/mci/{mciId}/vm/{vmId}", produces = "application/json")
  TumblebugMCI.Vm getVM(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String vmId);

  @GetMapping("/tumblebug/ns/{nsId}/resources/sshKey/{sshKeyId}")
  TumblebugSshKey getSshKey(@PathVariable String nsId, @PathVariable String sshKeyId);

  @GetMapping("/tumblebug/ns")
  TumblebugNS getNSList();

  @GetMapping("/tumblebug/ns/{nsId}/mci/{mciId}")
  TumblebugMCI getMCIList(@PathVariable String nsId, @PathVariable String mciId);

  @PostMapping("/tumblebug/ns/{nsId}/cmd/mci/{mciId}")
  Object sendCommand(@PathVariable String nsId, @PathVariable String mciId, @RequestParam String vmId,
      @RequestBody TumblebugCmd tumblebugCmd);
}












