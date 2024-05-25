package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.model.MetricDataParamInfo;
import mcmp.mc.observability.agent.model.MetricInfo;
import mcmp.mc.observability.agent.model.MetricParamInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final InfluxDBService influxDBService;

    public List<MetricInfo> getMetrics(MetricParamInfo metricParamInfo) {
        return influxDBService.getMetrics(metricParamInfo);
    }

    public List<MetricInfo> getMetricDatas(MetricDataParamInfo metricDataParamInfo) {
        return influxDBService.getMetricDatas(metricDataParamInfo);
    }
}
