package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.enums.ResultCode;
import mcmp.mc.observability.agent.model.MetricInfo;
import mcmp.mc.observability.agent.model.MetricsInfo;
import mcmp.mc.observability.agent.model.dto.ResBody;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final InfluxDBService influxDBService;

    public ResBody<List<MetricInfo>> getMetrics(MetricsInfo metricsInfo) {
        metricsInfo.convertObject();

        ResBody<List<MetricInfo>> res = new ResBody<>();

        if( !metricsInfo.isVaild() ) {
            res.setCode(ResultCode.INVAILD_PARAMETER);
            return res;
        }

        res.setData(influxDBService.getMetrics(metricsInfo));

        return res;
    }
}
