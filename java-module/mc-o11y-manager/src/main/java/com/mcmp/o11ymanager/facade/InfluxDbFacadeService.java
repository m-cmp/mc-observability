package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.config.InfluxDbInfo;
import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.global.target.ResBody;
import com.mcmp.o11ymanager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfluxDbFacadeService {

  private final InfluxDbService influxDbService;
  private final TargetService targetService;
  ;

  public InfluxDbInfo.Server resolveForTarget(String nsId, String mciId) {
    int seq = getInfluxSeq(nsId, mciId);    // 0-based seq
    return influxDbService.resolveByNo(seq);
  }


  public ResBody<List<TagDTO>> getTags() {
    return influxDbService.getTags();
  }

  public ResBody<List<FieldDTO>> getFields() {
    return influxDbService.getFields();
  }


  public int getInfluxSeq(String nsId, String mciId) {
    var targets = targetService.getByNsMci(nsId, mciId);

    if (targets == null || targets.isEmpty()) {
      throw new IllegalArgumentException("Target not found: ns=" + nsId + ", mci=" + mciId);
    }

    var t = targets.get(0);

    Integer seq = t.toEntity().getInfluxSeq();
    if (seq == null) {
      throw new IllegalStateException("influxSeq not assigned: ns=" + nsId + ", mci=" + mciId);
    }
    return seq;
  }


  public ResBody<InfluxDTO> getInflux(String nsId, String mciId) {
    int seq = getInfluxSeq(nsId, mciId);
    var s = influxDbService.resolveByNo(seq);

    InfluxDTO dto = new InfluxDTO();
    dto.setSeq(seq);
    dto.setUrl(s.url());
    dto.setDatabase(s.database());
    dto.setUsername(s.username());
    dto.setPassword(s.password());
    dto.setRetention_policy(influxDbService.fetchDefaultRp(s));

    ResBody<InfluxDTO> res = new ResBody<>();
    res.setData(dto);
    return res;

  }

  public ResBody<List<InfluxDTO>> getInfluxDbs() {
    return influxDbService.getList();
  }


}
