package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.entity.InfluxDbInfo;

public interface InfluxDbService {

  boolean isConnectedDb(InfluxDbInfo.Server s);

  int resolveInfluxDb(String nsId, String mciId);

  int getInflux(String nsId, String mciId);

  InfluxDbInfo.Server resolveByNo(int influxNo);

  InfluxDbInfo.Server resolveForTarget(String nsId, String mciId);

}
