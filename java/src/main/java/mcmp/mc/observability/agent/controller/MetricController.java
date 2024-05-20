package mcmp.mc.observability.agent.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.model.InfluxDBConnector;
import mcmp.mc.observability.agent.model.dto.ResBody;
import mcmp.mc.observability.agent.service.MetricService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(Constants.PREFIX_V1 + "/host/metric")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @ApiOperation(value = "")
    @GetMapping("/info")
    public ResBody getMeasurementAndFields(@ModelAttribute("influxDBConnector") InfluxDBConnector influxDBConnector) {
        ResBody res = new ResBody();
        res.setData(metricService.getMeasurementAndFields(influxDBConnector));
        return res;
    }
}
