package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64Decode;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64Encode;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.model.TargetInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.service.TargetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.MONITORING_URI)
public class TargetController {

    private final TargetService targetService;

    @Base64Encode
    @GetMapping("/target")
    public ResBody<List<TargetInfo>> list() {
        return targetService.getList();
    }

    @Base64Encode
    @GetMapping("/{nsId}/{mciId}/target")
    public ResBody<List<TargetInfo>> list(@PathVariable String nsId, @PathVariable String mciId) {
        return targetService.getList(nsId, mciId);
    }

    @Base64Encode
    @GetMapping("/{nsId}/{mciId}/target/{targetId}")
    public ResBody<TargetInfo> getTarget(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return targetService.getTarget(nsId, mciId, targetId);
    }

    @Base64Decode(TargetInfo.class)
    @PostMapping("/{nsId}/{mciId}/target/{targetId}")
    public ResBody insert(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody TargetInfo targetInfo) {
        return targetService.insert(nsId, mciId, targetId, targetInfo);
    }

    @Base64Decode(TargetInfo.class)
    @PutMapping("/{nsId}/{mciId}/target/{targetId}")
    public ResBody update(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody TargetInfo targetInfo) {
        return targetService.update(nsId, mciId, targetId, targetInfo);
    }

    @DeleteMapping("/{nsId}/{mciId}/target/{targetId}")
    public ResBody delete(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        return targetService.delete(nsId, mciId, targetId);
    }
}
