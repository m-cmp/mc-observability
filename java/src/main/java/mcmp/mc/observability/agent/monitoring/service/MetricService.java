package mcmp.mc.observability.agent.monitoring.service;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;
import mcmp.mc.observability.agent.monitoring.model.MetricInfo;
import mcmp.mc.observability.agent.monitoring.model.MetricsInfo;
import mcmp.mc.observability.agent.common.model.ResBody;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final InfluxDBService influxDBService;

    public ResBody<List<MetricInfo>> getMetrics(MetricsInfo metricsInfo) {
        ResBody<List<MetricInfo>> res = new ResBody<>();

        if( !metricsInfo.isVaild() ) {
            res.setCode(ResultCode.INVAILD_PARAMETER);
            return res;
        }

        res.setData(influxDBService.getMetrics(metricsInfo));

        return res;
    }
}
