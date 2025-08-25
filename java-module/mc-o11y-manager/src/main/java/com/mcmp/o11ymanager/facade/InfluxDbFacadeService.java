package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfluxDbFacadeService {

  private final InfluxDbService influxDbService;

  @Transactional(readOnly = true)
  public InfluxDTO resolveForTarget(String nsId, String mciId) {
    return influxDbService.resolveInfluxDto(nsId, mciId);
  }


  public List<TagDTO> getTags() {
    return influxDbService.getTags().getData();
  }

  public List<FieldDTO> getFields() {
    return influxDbService.getFields().getData();
  }



  public List<MetricDTO> getMetrics(String nsId, String mciId, MetricRequestDTO req) {
    return influxDbService.getMetrics(nsId, mciId, req);
  }


  public List<InfluxDTO> getInfluxDbs() {
    return influxDbService.rawServers();
  }



}
