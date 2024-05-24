package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.model.MetricDataParamInfo;
import mcmp.mc.observability.agent.model.MetricParamInfo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final InfluxDBService influxDBService;

    public Object getMetrics(MetricParamInfo metricParamInfo) {
        return influxDBService.getMetrics(metricParamInfo);
    }

    public Object getMetricDatas(MetricDataParamInfo metricDataParamInfo) {
        return influxDBService.getMetricDatas(metricDataParamInfo);
    }
}
