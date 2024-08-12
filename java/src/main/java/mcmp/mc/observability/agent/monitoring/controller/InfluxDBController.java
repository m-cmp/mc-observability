package mcmp.mc.observability.agent.monitoring.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.Constants;
import mcmp.mc.observability.agent.monitoring.model.InfluxDBInfo;
import mcmp.mc.observability.agent.monitoring.model.MeasurementFieldInfo;
import mcmp.mc.observability.agent.monitoring.model.MeasurementTagInfo;
import mcmp.mc.observability.agent.common.model.ResBody;
import mcmp.mc.observability.agent.monitoring.service.InfluxDBService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.MONITORING_URI + "/influxdb")
@RequiredArgsConstructor
public class InfluxDBController {

    private final InfluxDBService influxDBService;


    @ApiOperation(value = "Get InfluxDB list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Saved InfluxDB distinct connection information"),
            @ApiResponse(code = 404, message = "Not Found")
    })

    @GetMapping("/list")
    public ResBody<List<InfluxDBInfo>> getList() {
        return influxDBService.getList();
    }

    @ApiOperation(value = "Get InfluxDB measurement, field list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Select influxDB all measurement, field list"),
            @ApiResponse(code = 404, message = "Not Found")
    })
    @GetMapping("/fields/{influxDBSeq}")
    public ResBody<List<MeasurementFieldInfo>> getMeasurementAndFields(@PathVariable Long influxDBSeq) {
        return influxDBService.getFields(influxDBSeq);
    }

    @ApiOperation(value = "Get InfluxDB tag list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Select influxDB all measurement, tag list"),
            @ApiResponse(code = 404, message = "Not Found")
    })
    @GetMapping("/tags/{influxDBSeq}")
    public ResBody<List<MeasurementTagInfo>> getTags(@PathVariable Long influxDBSeq) {

        return influxDBService.getTags(influxDBSeq);
    }
}
