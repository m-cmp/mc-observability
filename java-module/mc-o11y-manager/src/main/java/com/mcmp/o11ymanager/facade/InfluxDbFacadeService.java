package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
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
    int serverIndex = influxDbService.resolveInfluxDb(nsId, mciId);
    List<InfluxDTO> servers = influxDbService.rawServers();
    return servers.get(serverIndex);
  }


  @Transactional
  public InfluxDTO postInflux(InfluxDTO influxDTO){

    InfluxDTO savedInflux;

    savedInflux = influxDbService.postDb(influxDTO);

    return savedInflux;
  }


  public List<TagDTO> getTags() {
    return influxDbService.getTags().getData();
  }

  public List<FieldDTO> getFields() {
    return influxDbService.getFields().getData();
  }



  public List<MetricDTO> getMetrics(MetricRequestDTO req) {
    return influxDbService.getMetrics(req);
  }


  public List<InfluxDTO> getInfluxDbs() {
    return influxDbService.rawServers();
  }




}
