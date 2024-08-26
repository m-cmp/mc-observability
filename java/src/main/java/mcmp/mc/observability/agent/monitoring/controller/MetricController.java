package mcmp.mc.observability.agent.monitoring.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.monitoring.model.MetricInfo;
import mcmp.mc.observability.agent.monitoring.model.MetricsInfo;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.service.MetricService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(Constants.MONITORING_URI + "/metric")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @ApiOperation(value = "Get metrics")
    @PostMapping("")
    public ResBody<List<MetricInfo>> getMetric(@Valid @RequestBody MetricsInfo metricsInfo) {
        return metricService.getMetrics(metricsInfo);
    }
}
