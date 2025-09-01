package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.manager.global.target.ResBody;
import java.util.List;

public interface InfluxDbService {

  InfluxDTO get(Long id);

  boolean isConnectedDb(InfluxDTO influxDTO);

  Long resolveInfluxDb(String nsId, String mciId);

  ResBody<List<FieldDTO>> getFields();

  ResBody<List<TagDTO>> getTags();

  String fetchDefaultRp(InfluxDTO influxDTO);

  List<MetricDTO> getMetrics(String nsId, String mciId, MetricRequestDTO req);

  List<InfluxDTO> rawServers();
  InfluxDTO resolveInfluxDto(String nsId, String mciId);

}
