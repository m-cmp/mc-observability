package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.facade.FileFacadeService;
import com.mcmp.o11ymanager.facade.InfluxDbFacadeService;
import com.mcmp.o11ymanager.global.target.ResBody;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/influxdb")
public class InfluxDBController {

  private final InfluxDbFacadeService influxDbFacadeService;


  @GetMapping
  public ResBody<List<InfluxDTO>> getAllInfluxDB()
  {
      return influxDbFacadeService.getInfluxDbs();
  }

  @GetMapping("/measurement")
  public ResBody<List<FieldDTO>> measurement() {
     return influxDbFacadeService.getFields();
  }

  @GetMapping("/tag")
  public ResBody<List<TagDTO>> tag() {
    return influxDbFacadeService.getTags();
  }

}
