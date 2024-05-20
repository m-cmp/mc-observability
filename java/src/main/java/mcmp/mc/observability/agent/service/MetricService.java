package mcmp.mc.observability.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.model.InfluxDBConnector;
import mcmp.mc.observability.agent.model.MeasurementFieldInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {

    private final InfluxDBService influxDBService;

    public List<MeasurementFieldInfo> getMeasurementAndFields(InfluxDBConnector influxDBConnector) {
        return influxDBService.getMeasurementAndFields(influxDBConnector);
    }

}
