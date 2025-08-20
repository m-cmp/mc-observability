package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.facade.InfluxDbFacadeService;
import com.mcmp.o11ymanager.global.target.ResBody;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/influxdb")
public class InfluxDBController {

  private final InfluxDbFacadeService influxDbFacadeService;


  @PostMapping("/storage")
  public ResBody<InfluxDTO> postInfluxDB(@RequestBody InfluxDTO req)
  {
    return new ResBody<>(influxDbFacadeService.postInflux(req));
  }


  @GetMapping
  public ResBody<List<InfluxDTO>> getAllInfluxDB()
  {
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


  @PostMapping("/metric")
  public ResBody<List<MetricDTO>> query(@RequestBody MetricRequestDTO req) {
    return new ResBody<>(influxDbFacadeService.getMetrics(req));
  }


}
