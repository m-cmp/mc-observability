package mcmp.mc.observability.agent.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.annotation.Base64Encode;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.model.MetricParamInfo;
import mcmp.mc.observability.agent.model.dto.ResBody;
import mcmp.mc.observability.agent.service.MetricService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(Constants.PREFIX_V1 + "/metric")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @ApiOperation(value = "Get Host storage all list")
    @Base64Encode
    @GetMapping("")
    public ResBody getMetrics(@Valid @ModelAttribute MetricParamInfo metricParamInfo) {
        ResBody res = new ResBody();
        res.setData(metricService.getMetrics(metricParamInfo));
        return res;
    }

}
