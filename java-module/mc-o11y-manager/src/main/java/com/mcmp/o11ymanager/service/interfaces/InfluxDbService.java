package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.global.target.ResBody;
import java.util.List;

public interface InfluxDbService {

  InfluxDTO postDb(InfluxDTO influxDTO);

  int resolveInfluxDb(String nsId, String mciId);

  ResBody<List<FieldDTO>> getFields();

  ResBody<List<TagDTO>> getTags();

  String fetchDefaultRp(InfluxDTO influxDTO);

  List<MetricDTO> getMetrics(MetricRequestDTO req);

  List<InfluxDTO> rawServers();

}
