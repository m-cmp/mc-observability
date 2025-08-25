package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.global.target.ResBody;
import java.util.List;

public interface InfluxDbService {

  boolean isConnectedDb(InfluxDTO influxDTO);


  Long resolveInfluxDb(String nsId, String mciId);

  int resolveInfluxDb2(String nsId, String mciId);

  ResBody<List<FieldDTO>> getFields();

  ResBody<List<TagDTO>> getTags();

  String fetchDefaultRp(InfluxDTO influxDTO);

  List<MetricDTO> getMetrics(String nsId, String mciId, MetricRequestDTO req);

  List<InfluxDTO> rawServers();
  InfluxDTO resolveInfluxDto(String nsId, String mciId);

}
