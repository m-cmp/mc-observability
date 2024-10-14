package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.InfluxDBMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.InfluxDBInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MeasurementFieldInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MeasurementTagInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MetricInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MetricsInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.service.InfluxDBService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.MONITORING_URI + "/influxdb")
public class InfluxDBController {

    private final InfluxDBService influxDBService;
    private final InfluxDBMapper influxDBMapper;

    @GetMapping
    public ResBody<List<InfluxDBInfo>> list() {
        return influxDBService.getList();
    }

    @GetMapping("/measurement")
    public ResBody<List<MeasurementFieldInfo>> measurement() {
        InfluxDBInfo influxDBInfo = influxDBMapper.getInfluxDBInfoList().get(1);
        return influxDBService.getFields(influxDBInfo);
    }

    @GetMapping("/tag")
    public ResBody<List<MeasurementTagInfo>> tag() {
        InfluxDBInfo influxDBInfo = influxDBMapper.getInfluxDBInfoList().get(1);
        return influxDBService.getTags(influxDBInfo);
    }

    @PostMapping("/metric")
    public ResBody<List<MetricInfo>> metric(@RequestBody MetricsInfo metricsInfo) {
        ResBody<List<MetricInfo>> resBody = new ResBody<>();
        InfluxDBInfo influxDBInfo = influxDBMapper.getInfluxDBInfoList().get(1);
        resBody.setData(influxDBService.getMetrics(influxDBInfo, metricsInfo));
        return resBody;
    }
}
