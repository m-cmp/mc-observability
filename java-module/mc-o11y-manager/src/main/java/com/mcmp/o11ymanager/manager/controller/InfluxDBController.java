package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.manager.facade.InfluxDbFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/influxdb")
public class InfluxDBController {

    private final InfluxDbFacadeService influxDbFacadeService;

    @GetMapping
    public ResBody<List<InfluxDTO>> getAllInfluxDB() {
        return new ResBody<>(influxDbFacadeService.getInfluxDbs());
    }

    @GetMapping("/measurement")
    public ResBody<List<FieldDTO>> measurement() {
        return new ResBody<>(influxDbFacadeService.getFields());
    }

    @GetMapping("/tag")
    public ResBody<List<TagDTO>> tag() {
        return new ResBody<>(influxDbFacadeService.getTags());
    }


    @PostMapping("/metric/{nsId}/{mciId}")
    public ResBody<List<MetricDTO>> MetricByNsIdAndMciId(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @RequestBody MetricRequestDTO req) {
        return new ResBody<>(influxDbFacadeService.postMetricsByNsMci(nsId, mciId, req));
    }


  @PostMapping("/metric/{nsId}/{mciId}/{vmId}")
  public ResBody<List<MetricDTO>> MetricByVMId(
      @PathVariable String nsId,
      @PathVariable String mciId,
      @PathVariable String vmId,
      @RequestBody MetricRequestDTO req) {
    return new ResBody<>(influxDbFacadeService.postMetricsByVM(nsId, mciId, vmId, req));
  }
}
