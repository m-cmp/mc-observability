package mcmp.mc.observability.mco11yagent.monitoring.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64Encode;
import mcmp.mc.observability.mco11yagent.monitoring.common.Constants;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;
import mcmp.mc.observability.mco11yagent.monitoring.model.*;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import mcmp.mc.observability.mco11yagent.monitoring.service.MonitoringService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.MONITORING_URI + "/{nsId}/{mciId}/target/{targetId}/csp")
public class CSPController {

    private final MonitoringService monitoringService;

    @Base64Encode
    @GetMapping("/{measurement}")
    public ResBody<SpiderMonitoringInfo.Data> getCSP(@PathVariable String nsId, @PathVariable String mciId, @PathVariable String targetId, @PathVariable String measurement) {
        ResBody<SpiderMonitoringInfo.Data> resBody = new ResBody<>();
        try {
            resBody.setData(monitoringService.geSpiderVMMonitoring(nsId, mciId, targetId, measurement, "1", "1"));
        } catch (Exception e) {
            resBody.setCode(ResultCode.FAILED);
            resBody.setErrorMessage(e.getMessage());
        }
        return resBody;
    }
}
