package mcmp.mc.observability.mco11yagent.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.PluginMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.InfluxDBInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.PluginDefInfo;
import mcmp.mc.observability.mco11yagent.monitoring.client.SpiderClient;
import mcmp.mc.observability.mco11yagent.monitoring.client.TumblebugClient;
import mcmp.mc.observability.mco11yagent.monitoring.model.TumblebugMCI;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {
    private final TumblebugClient tumblebugClient;
    private final InfluxDBService influxDBService;
    private final PluginMapper pluginMapper;

    public void writeSpiderVMMonitoring(InfluxDBInfo influxDBinfo, String nsId, String mciId, String targetId, String timeBeforeHour, String intervalMinute) {
        TumblebugMCI.Vm vm = tumblebugClient.getVM(nsId, mciId, targetId);

        try {
            List<PluginDefInfo> pluginList = pluginMapper.getList();

            for (PluginDefInfo plugin : pluginList) {
                influxDBService.writeMetrics(vm, influxDBinfo, plugin.getName(), timeBeforeHour,intervalMinute);
            }

        } catch (Exception e) {
            log.error(ExceptionUtils.getMessage(e));
        }

    }
}
