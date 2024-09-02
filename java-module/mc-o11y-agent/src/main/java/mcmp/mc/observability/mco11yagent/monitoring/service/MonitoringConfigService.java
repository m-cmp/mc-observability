package mcmp.mc.observability.mco11yagent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.MonitoringConfigMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.MonitoringConfigInfo;
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

    public List<MonitoringConfigInfo> list(String nsId, String targetId) {
        return monitoringConfigMapper.getList(nsId, targetId);
    }

    public ResBody insert(String nsId, String targetId, MonitoringConfigInfo monitoringConfigInfo) {
        monitoringConfigInfo.setNsId(nsId);
        monitoringConfigInfo.setTargetId(targetId);
        monitoringConfigMapper.insert(monitoringConfigInfo);
        return ResBody.builder().build();
    }

    public ResBody update(String nsId, String targetId, MonitoringConfigInfo monitoringConfigInfo) {
        MonitoringConfigInfo originalConfig = monitoringConfigMapper.getDetail(monitoringConfigInfo.getSeq());
        monitoringConfigInfo.setNsId(nsId);
        monitoringConfigInfo.setTargetId(targetId);
        monitoringConfigInfo.setState("UPDATE");
        monitoringConfigMapper.update(monitoringConfigInfo);

        if("influxdb".equals(monitoringConfigInfo.getPluginName())) {
            triggerTaskManageService.manageStorage(originalConfig, monitoringConfigInfo);
        }
        return ResBody.builder().build();
    }

    public ResBody delete(String nsId, String targetId, Long seq) {
        MonitoringConfigInfo monitoringConfigInfo = new MonitoringConfigInfo();
        monitoringConfigInfo.setNsId(nsId);
        monitoringConfigInfo.setTargetId(targetId);
        monitoringConfigInfo.setSeq(seq);
        monitoringConfigInfo.setState("DELETE");
        monitoringConfigMapper.update(monitoringConfigInfo);
        return ResBody.builder().build();
    }

    public int updateState(MonitoringConfigInfo monitoringConfigInfo, String state) {
        monitoringConfigInfo.setState(state);
        return monitoringConfigMapper.updateState(monitoringConfigInfo);
    }
}
