package mcmp.mc.observability.agent.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.model.InfluxDBConnector;
import mcmp.mc.observability.agent.model.InfluxDBInfo;
import mcmp.mc.observability.agent.model.MeasurementFieldInfo;
import mcmp.mc.observability.agent.model.dto.ResBody;
import mcmp.mc.observability.agent.service.InfluxDBService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(Constants.PREFIX_V1 + "/influxdb")
@RequiredArgsConstructor
public class InfluxDBController {

    private final InfluxDBService influxDBService;


    @ApiOperation(value = "Get InfluxDB list")
    @GetMapping("/list")
    public ResBody<List<InfluxDBInfo>> getList() {
        ResBody<List<InfluxDBInfo>> res = new ResBody<>();
        res.setData(influxDBService.getList());
        return res;
    }

    @ApiOperation(value = "Get InfluxDB measurement, field list")
    @GetMapping("/info")
    public ResBody<List<MeasurementFieldInfo>> getMeasurementAndFields(@ModelAttribute InfluxDBInfo influxDBInfo) {
        ResBody<List<MeasurementFieldInfo>> res = new ResBody<>();
        res.setData(influxDBService.getMeasurementAndFields(new InfluxDBConnector(influxDBInfo)));
        return res;
    }

    @ApiOperation(value = "Get InfluxDB tag list")
    @GetMapping("/tags")
    public ResBody<List<Map<String, Object>>> getTags(@ModelAttribute InfluxDBInfo influxDBInfo) {
        ResBody<List<Map<String, Object>>> res = new ResBody<>();
        res.setData(influxDBService.getTags(new InfluxDBConnector(influxDBInfo)));
        return res;
    }
}
