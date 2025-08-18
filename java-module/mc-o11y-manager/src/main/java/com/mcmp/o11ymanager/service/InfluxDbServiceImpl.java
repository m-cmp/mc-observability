package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.entity.InfluxDbInfo;
import com.mcmp.o11ymanager.repository.TargetJpaRepository;
import com.mcmp.o11ymanager.service.interfaces.InfluxDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfluxDbServiceImpl implements InfluxDbService {


  private final InfluxDbInfo influxDbInfo;
  private final TargetJpaRepository targetJpaRepository;

  private static final String NS_ID = "ns_id";
  private static final String MCI_ID = "mci_id";

  @Override
  public boolean isConnectedDb(InfluxDbInfo.Server s) {
    try (var influx = org.influxdb.InfluxDBFactory.connect(s.url(), s.username(), s.password())) {
      var pong = influx.ping();
      return pong != null && !"unknown".equalsIgnoreCase(pong.getVersion());
    } catch (Exception e) {
      log.info(
          "==========================[INFLUX CONNECTION TEST] failed url={}, err={}==========================",
          s.url(), e.toString());
      return false;
    }
  }

  @Override
  public int resolveInfluxDb(String nsId, String mciId) {
    var servers = influxDbInfo.servers();
    if (servers == null || servers.isEmpty()) {
      throw new IllegalStateException("influxgdb.servers must contain at least 1 server");
    }

    final int ERROR = Integer.MAX_VALUE;
    int bestSeq = -1;
    int bestVal = ERROR;

    log.info(
        "==========================[INF-SELECT] start ns={}, mci={}, servers={}==========================",
        nsId, mciId, servers.size());

    for (int i = 0; i < servers.size(); i++) {
      var s = servers.get(i);

      if (!isConnectedDb(s)) {
        log.info(
            "==========================[INFLUX CONNECTION TEST] idx={}, url={} -> PING FAIL (excluded)==========================",
            i + 1, s.url());
        continue;
      }

      int v = calTargetCnt(s, nsId, mciId);

      if (v == ERROR) {
        log.warn("[INF-SELECT] idx={}, url={} -> QUERY ERROR (excluded)", i + 1, s.url());
        continue;
      }

      log.info("[INF-SELECT] idx={}, url={} -> count={}", i + 1, s.url(), v);

      if (bestSeq == -1 || v < bestVal) {
        bestVal = v;
        bestSeq = i; // 0-based
        log.info(
            "==================================[INF-SELECT] new best: seq={}, url={}, count={}==================================",
            bestSeq, s.url(), v);
      }
    }

    if (bestSeq == -1) {
      throw new IllegalStateException(
          "no reachable influxdb candidates for ns=" + nsId + ", mci=" + mciId);
    }

    log.info("[INF-SELECT] final choice: seq={}, url={}, count={}",
        bestSeq, servers.get(bestSeq - 1).url(), bestVal);
    return bestSeq;
  }


  @Override
  public int getInflux(String nsId, String mciId) {
    var t = targetJpaRepository.findByNsIdAndMciId(nsId, mciId)
        .stream().findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Target not found: ns=" + nsId + ", mci=" + mciId));
    Integer seq = t.getInfluxSeq();
    if (seq == null) {
      throw new IllegalStateException("can not found InfluxDB seq");
    }
    return seq;
  }


  @Override
  public InfluxDbInfo.Server resolveByNo(int influxNo) {
    var servers = influxDbInfo.servers();
    if (servers == null || servers.size() < influxNo) {
      throw new IllegalStateException("required influx db set up, seq=" + influxNo);
    }
    var s = servers.get(influxNo - 1);
    return new InfluxDbInfo.Server(
        trimSlash(s.url()),
        s.database(),
        s.username(),
        s.password()
    );
  }

  @Override
  public InfluxDbInfo.Server resolveForTarget(String nsId, String mciId) {
    var t = targetJpaRepository.findByNsIdAndMciId(nsId, mciId)
        .stream().findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Target not found: ns=" + nsId + ", mci=" + mciId));

    Integer seq = t.getInfluxSeq();
    if (seq == null) {
      throw new IllegalStateException("influxSeq not assigned: ns=" + nsId + ", mci=" + mciId);
    }
    return resolveByNo(seq);
  }


  private int calTargetCnt(InfluxDbInfo.Server s, String nsId, String mciId) {
    String escNs = nsId == null ? "" : nsId.replace("'", "\\'");
    String escMci = mciId == null ? "" : mciId.replace("'", "\\'");

    String q = "SHOW SERIES CARDINALITY ON \"" + s.database() + "\" "
        + "WHERE \"" + NS_ID + "\"='" + escNs + "' AND \"" + MCI_ID + "\"='" + escMci + "'";

    try (var influx = org.influxdb.InfluxDBFactory.connect(s.url(), s.username(), s.password())) {
      var qr = influx.query(new org.influxdb.dto.Query(q, s.database()));

      if (qr == null || hasError(qr)) {
        return Integer.MAX_VALUE;
      }

      var results = qr.getResults();
      if (results == null || results.isEmpty() || results.get(0) == null) {
        return 0;
      }
      var r0 = results.get(0);
      var seriesList = r0.getSeries();
      if (seriesList == null || seriesList.isEmpty()) {
        return 0;
      }
      var values = seriesList.get(0).getValues();
      if (values == null || values.isEmpty() || values.get(0) == null) {
        return 0;
      }

      var row = values.get(0);
      Object val = row.get(row.size() - 1);
      if (val == null) {
        return 0; // 값 없음 = 여유
      }

      if (val instanceof Number num) {
        long l = num.longValue();
        return l > Integer.MAX_VALUE ? Integer.MAX_VALUE - 1 : (int) l;
      }

      try {
        long l = Long.parseLong(val.toString());
        return l > Integer.MAX_VALUE ? Integer.MAX_VALUE - 1 : (int) l;
      } catch (NumberFormatException e) {
        return Integer.MAX_VALUE;
      }
    } catch (Exception e) {
      log.warn("cardinality query failed url={}, db={}, ns={}, mci={}, err={}",
          s.url(), s.database(), nsId, mciId, e.toString());
      return Integer.MAX_VALUE;
    }
  }

  private boolean hasError(org.influxdb.dto.QueryResult qr) {
    if (qr.getError() != null) {
      return true;
    }
    var results = qr.getResults();
    if (results == null) {
      return false;
    }
    for (var r : results) {
      if (r != null && r.getError() != null) {
        return true;
      }
    }
    return false;
  }


  private String trimSlash(String url) {
    return (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
  }


}
