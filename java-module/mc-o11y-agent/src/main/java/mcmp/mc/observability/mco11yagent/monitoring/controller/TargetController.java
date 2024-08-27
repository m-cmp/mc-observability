package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
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
@RequestMapping(Constants.PREFIX_V1)
public class TargetController {

    private final TargetService targetService;

    @GetMapping("/target")
    public ResBody<List<TargetInfo>> list() {
        return targetService.getList();
    }

    @PostMapping("/{nsId}/target/{targetId}")
    public ResBody insert(@PathVariable String nsId, @PathVariable String targetId) {
        return targetService.insert(nsId, targetId);
    }

    @PutMapping("/{nsId}/target/{targetId}")
    public ResBody update(@PathVariable String nsId, @PathVariable String targetId, @RequestBody TargetInfo targetInfo) {
        return targetService.update(nsId, targetId, targetInfo);
    }

    @DeleteMapping("/{nsId}/target/{targetId}")
    public ResBody delete(@PathVariable String nsId, @PathVariable String targetId) {
        return targetService.delete(nsId, targetId);
    }
}
