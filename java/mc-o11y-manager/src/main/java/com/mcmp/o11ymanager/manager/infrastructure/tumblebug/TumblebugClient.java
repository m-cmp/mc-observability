package com.mcmp.o11ymanager.manager.infrastructure.tumblebug;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfra;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfraList;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "cb-tumblebug",
        url = "${feign.cb-tumblebug.url}",
        configuration = TumblebugFeignConfig.class)
public interface TumblebugClient {
    @GetMapping(
            value = "/tumblebug/ns/{nsId}/infra/{infraId}/node/{nodeId}",
            produces = "application/json")
    TumblebugInfra.Node getNode(
            @PathVariable String nsId, @PathVariable String infraId, @PathVariable String nodeId);

    @GetMapping("/tumblebug/ns/{nsId}/resources/sshKey/{sshKeyId}")
    TumblebugSshKey getSshKey(@PathVariable String nsId, @PathVariable String sshKeyId);

    @GetMapping("/tumblebug/ns")
    TumblebugNS getNSList();

    @GetMapping("/tumblebug/ns/{nsId}/infra")
    TumblebugInfraList getInfraList(@PathVariable String nsId);

    @GetMapping("/tumblebug/ns/{nsId}/infra/{infraId}")
    TumblebugInfra getInfra(@PathVariable String nsId, @PathVariable String infraId);

    @PostMapping("/tumblebug/ns/{nsId}/cmd/infra/{infraId}")
    Object sendCommand(
            @PathVariable String nsId,
            @PathVariable String infraId,
            @RequestParam String nodeId,
            @RequestBody TumblebugCmd tumblebugCmd);
}
