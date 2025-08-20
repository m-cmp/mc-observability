package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.config.InfluxDbInfo;
import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.global.target.ResBody;
import java.util.List;

public interface InfluxDbService {

  ResBody<List<InfluxDTO>> getList();

  ResBody<List<TagDTO>> getTags();

  ResBody<List<FieldDTO>> getFields();

  boolean isConnectedDb(InfluxDbInfo.Server s);

  int resolveInfluxDb(String nsId, String mciId);

  String fetchDefaultRp(InfluxDbInfo.Server s);

  InfluxDbInfo.Server resolveByNo(int influxNo);
}
