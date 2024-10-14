package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64Decode;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64Encode;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.monitoring.model.MonitoringConfigInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.MonitoringConfigInfoCreateDTO;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.MonitoringConfigInfoUpdateDTO;
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
@RequestMapping(Constants.MONITORING_URI + "/{nsId}/{mciId}/target/{targetId}/storage")
public class StorageController {

    private final MonitoringConfigService monitoringConfigService;

    @Base64Encode
    @GetMapping
    public ResBody<List<MonitoringConfigInfo>> list(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId) {
        List<MonitoringConfigInfo> list = monitoringConfigService.list(nsId, mciId, targetId);
        ResBody<List<MonitoringConfigInfo>> resBody = new ResBody<>();
        resBody.setData(list.stream().filter(f -> f.getPluginType().equals("OUTPUT")).collect(Collectors.toList()));
        return resBody;
    }

    @Base64Decode(MonitoringConfigInfo.class)
    @PostMapping
    public ResBody insert(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfoCreateDTO storageCreateInfo) {
        MonitoringConfigInfo monitoringConfigInfo = new MonitoringConfigInfo();
        monitoringConfigInfo.setNsId(nsId);
        monitoringConfigInfo.setMciId(mciId);
        monitoringConfigInfo.setTargetId(targetId);
        monitoringConfigInfo.setName(storageCreateInfo.getName());
        monitoringConfigInfo.setPluginSeq(storageCreateInfo.getPluginSeq());
        monitoringConfigInfo.setPluginName(storageCreateInfo.getPluginName());
        monitoringConfigInfo.setPluginType(storageCreateInfo.getPluginType());
        monitoringConfigInfo.setPluginConfig(storageCreateInfo.getPluginConfig());


        return monitoringConfigService.insert(nsId, mciId, targetId, monitoringConfigInfo);
    }

    @Base64Decode(MonitoringConfigInfo.class)
    @PutMapping
    public ResBody update(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @RequestBody MonitoringConfigInfoUpdateDTO storageUpdateInfo) {
        MonitoringConfigInfo monitoringConfigInfo = new MonitoringConfigInfo();
        monitoringConfigInfo.setSeq(storageUpdateInfo.getSeq());
        monitoringConfigInfo.setNsId(nsId);
        monitoringConfigInfo.setMciId(mciId);
        monitoringConfigInfo.setTargetId(targetId);
        monitoringConfigInfo.setName(storageUpdateInfo.getName());
        monitoringConfigInfo.setPluginSeq(storageUpdateInfo.getPluginSeq());
        monitoringConfigInfo.setPluginName(storageUpdateInfo.getPluginName());
        monitoringConfigInfo.setPluginType(storageUpdateInfo.getPluginType());
        monitoringConfigInfo.setPluginConfig(storageUpdateInfo.getPluginConfig());

        return monitoringConfigService.update(nsId, mciId, targetId, monitoringConfigInfo);
    }

    @DeleteMapping("/{storageSeq}")
    public ResBody delete(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable Long storageSeq) {
        MonitoringConfigInfo monitoringConfigInfo = new MonitoringConfigInfo();
        monitoringConfigInfo.setSeq(storageSeq);
        if( monitoringConfigService.updateState(monitoringConfigInfo, "DELETE") <= 0 ) {
            return ResBody.builder().code(ResultCode.FAILED).build();
        }
        return ResBody.builder().build();
    }
}
