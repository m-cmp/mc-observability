package mcmp.mc.observability.mco11yagent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.MonitoringConfigMapper;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.PluginMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.MonitoringConfigInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.PluginDefInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.trigger.service.TriggerTaskManageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringConfigService {
    private final MonitoringConfigMapper monitoringConfigMapper;
    private final TriggerTaskManageService triggerTaskManageService;
    private final PluginMapper pluginMapper;

    public List<MonitoringConfigInfo> list(String nsId, String mciId, String targetId) {
        return monitoringConfigMapper.getList(nsId, mciId, targetId);
    }

    public ResBody insert(String nsId, String mciId, String targetId, MonitoringConfigInfo monitoringConfigInfo) {
        monitoringConfigInfo.setNsId(nsId);
        monitoringConfigInfo.setMciId(mciId);
        monitoringConfigInfo.setTargetId(targetId);

        PluginDefInfo pluginDefInfo = pluginMapper.getPlugin(monitoringConfigInfo.getPluginSeq());
        monitoringConfigInfo.setPluginName(pluginDefInfo.getName());
        monitoringConfigInfo.setPluginType(pluginDefInfo.getPluginType());

        monitoringConfigInfo.setState("ADD");
        monitoringConfigMapper.insert(monitoringConfigInfo);
        return ResBody.builder().build();
    }

    public ResBody update(String nsId, String mciId, String targetId, MonitoringConfigInfo monitoringConfigInfo) {
        MonitoringConfigInfo originalConfig = monitoringConfigMapper.getDetail(monitoringConfigInfo.getSeq());
        monitoringConfigInfo.setNsId(nsId);
        monitoringConfigInfo.setMciId(mciId);
        monitoringConfigInfo.setTargetId(targetId);

        PluginDefInfo pluginDefInfo = pluginMapper.getPlugin(monitoringConfigInfo.getPluginSeq());
        monitoringConfigInfo.setPluginName(pluginDefInfo.getName());
        monitoringConfigInfo.setPluginType(pluginDefInfo.getPluginType());

        monitoringConfigInfo.setState("UPDATE");
        monitoringConfigMapper.update(monitoringConfigInfo);

        if("influxdb".equals(monitoringConfigInfo.getPluginName())) {
            triggerTaskManageService.updateStorage(originalConfig, monitoringConfigInfo);
        }
        return ResBody.builder().build();
    }

    public ResBody delete(Long seq) {
        MonitoringConfigInfo monitoringConfigInfo = monitoringConfigMapper.getDetail(seq);
        monitoringConfigInfo.setState("DELETE");
        monitoringConfigMapper.update(monitoringConfigInfo);

        if("influxdb".equals(monitoringConfigInfo.getPluginName())) {
            triggerTaskManageService.deleteStorage(monitoringConfigInfo);
        }
        return ResBody.builder().build();
    }

    public int updateState(MonitoringConfigInfo monitoringConfigInfo, String state) {
        monitoringConfigInfo.setState(state);
        return monitoringConfigMapper.updateState(monitoringConfigInfo);
    }
}
