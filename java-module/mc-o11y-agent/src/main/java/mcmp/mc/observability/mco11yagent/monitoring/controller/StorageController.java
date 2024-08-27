package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.model.MonitoringConfigInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.service.MonitoringConfigService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.PREFIX_V1 + "/{nsId}/target/{targetId}/storage")
public class StorageController {

    private final MonitoringConfigService monitoringConfigService;

    @GetMapping
    public ResBody<List<MonitoringConfigInfo>> list(@PathVariable String nsId, @PathVariable String targetId) {
        List<MonitoringConfigInfo> list = monitoringConfigService.list(nsId, targetId);
        ResBody<List<MonitoringConfigInfo>> resBody = new ResBody<>();
        resBody.setData(list.stream().filter(f -> f.getPluginType().equals("OUTPUT")).collect(Collectors.toList()));
        return resBody;
    }

    @PostMapping
    public ResBody insert(@PathVariable String nsId, @PathVariable String targetId, @RequestBody MonitoringConfigInfo monitoringConfigInfo) {
        return monitoringConfigService.insert(nsId, targetId, monitoringConfigInfo);
    }

    @PutMapping
    public ResBody update(@PathVariable String nsId, @PathVariable String targetId, @RequestBody MonitoringConfigInfo monitoringConfigInfo) {
        return monitoringConfigService.update(nsId, targetId, monitoringConfigInfo);
    }

    @DeleteMapping("/{storageSeq}")
    public ResBody delete(@PathVariable String nsId, @PathVariable String targetId, @PathVariable Long storageSeq) {
        return monitoringConfigService.delete(nsId, targetId, storageSeq);
    }
}
