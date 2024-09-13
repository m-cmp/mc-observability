package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.model.MeasurementFieldInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MeasurementTagInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MetricInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MetricsInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MiningDBInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.MiningDBSetDTO;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.service.InfluxDBService;
import mcmp.mc.observability.mco11yagent.monitoring.service.MiningDBService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.MONITORING_URI + "/miningdb")
@RequiredArgsConstructor
public class MiningDBController {

    private final MiningDBService miningDBService;
    private final InfluxDBService influxDBService;

    @GetMapping
    public ResBody<MiningDBInfo> detail() {
        return miningDBService.detail();
    }

    @PutMapping
    public ResBody<Void> updateMiningDB(@RequestBody MiningDBSetDTO info) {
        return miningDBService.updateMiningDB(info);
    }

    @GetMapping("/measurement")
    public ResBody<List<MeasurementFieldInfo>> measurement() {
        return influxDBService.getFields();
    }

    @GetMapping("/tag")
    public ResBody<List<MeasurementTagInfo>> tag() {
        return influxDBService.getTags();
    }

    @PostMapping("/metric")
    public ResBody<List<MetricInfo>> metric(@RequestBody MetricsInfo metricsInfo) {
        ResBody<List<MetricInfo>> resBody = new ResBody<>();
        resBody.setData(influxDBService.getMetrics(metricsInfo));
        return resBody;
    }
}
