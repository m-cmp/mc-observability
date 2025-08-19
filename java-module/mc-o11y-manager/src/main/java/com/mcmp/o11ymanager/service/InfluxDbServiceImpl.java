package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.entity.InfluxDbInfo;
import com.mcmp.o11ymanager.repository.TargetJpaRepository;
import com.mcmp.o11ymanager.service.interfaces.InfluxDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
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
      throw new IllegalStateException("influxdb.servers must contain at least 1 server");
    }

    log.info("[INF-RESOLVE] start ns={}, mci={}, servers={}", nsId, mciId, servers.size());

    for (int i = 0; i < Math.min(2, servers.size()); i++) {
      var s = servers.get(i);
      if (!isConnectedDb(s)) {
        log.info("[INF-RESOLVE] idx={}, url={} PING FAIL -> skip", i, s.url());
        continue;
      }
      if (existsTagCombination(s, nsId, mciId)) {
        log.info("[INF-RESOLVE] found target combination at idx={}, url={}", i, s.url());
        return i; // 0-based
      }
    }

    for (int i = 0; i < Math.min(2, servers.size()); i++) {
      var s = servers.get(i);
      if (!isConnectedDb(s)) {
        log.info("[INF-RESOLVE] idx={}, url={} PING FAIL -> skip ns-only", i, s.url());
        continue;
      }
      if (existsNsId(s, nsId)) {
        log.info("[INF-RESOLVE] found nsId at idx={}, url={}", i, s.url());
        return i; // 0-based
      }
    }

    for (int i = 0; i < Math.min(2, servers.size()); i++){
      var s = servers.get(i);
      if (isConnectedDb(s)) {
        log.info("[INF-RESOLVE] fallback to first reachable idx={}, url={}", i, s.url());
        return i; // 0-based
      } else {
        log.info("[INF-RESOLVE] idx={}, url={} PING FAIL -> skip fallback", i, s.url());
      }
    }

    throw new IllegalStateException("no reachable influxdb candidates (ns=" + nsId + ", mci=" + mciId + ")");
  }



  @Override
  public int getInflux(String nsId, String mciId) {
    var t = targetJpaRepository.findByNsIdAndMciId(nsId, mciId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Target not found: ns=" + nsId + ", mci=" + mciId));

    Integer seq = t.getInfluxSeq();
    if (seq == null) {
      throw new IllegalStateException("influxSeq not assigned: ns=" + nsId + ", mci=" + mciId);
    }
    return seq; // 0-based
  }


  @Override
  public InfluxDbInfo.Server resolveByNo(int influxNo) {
    var servers = influxDbInfo.servers();
    if (servers == null) {
      throw new IllegalStateException("influxdb.servers not configured");
    }

    if (influxNo < 0 || influxNo >= servers.size()) {
      throw new IllegalStateException("invalid influx seq (0-based): " + influxNo);
    }
    var s = servers.get(influxNo);
    return new InfluxDbInfo.Server(
        trimSlash(s.url()),
        s.database(),
        s.username(),
        s.password()
    );
  }

  @Override
  public InfluxDbInfo.Server resolveForTarget(String nsId, String mciId) {
    int seq = getInflux(nsId, mciId);    // 0-based seq
    return resolveByNo(seq);
  }




  private boolean existsTagCombination(InfluxDbInfo.Server s, String nsId, String mciId) {
    String q = "SHOW TAG VALUES ON \"" + s.database() + "\" "
        + "WITH KEY=\"" + NS_ID + "\" "
        + "WHERE \"" + NS_ID + "\"='" + esc(nsId) + "' AND \"" + MCI_ID + "\"='" + esc(mciId) + "' "
        + "LIMIT 1";
    return hasResult(s, q);
  }


  private boolean existsNsId(InfluxDbInfo.Server s, String nsId) {
    String q = "SHOW TAG VALUES ON \"" + s.database() + "\" "
        + "WITH KEY=\"" + NS_ID + "\" "
        + "WHERE \"" + NS_ID + "\"='" + esc(nsId) + "' "
        + "LIMIT 1";
    return hasResult(s, q);
  }



  private boolean hasResult(InfluxDbInfo.Server s, String query) {
    try (var influx = InfluxDBFactory.connect(s.url(), s.username(), s.password())) {
      var qr = influx.query(new Query(query, s.database()));
      if (qr == null || hasError(qr)) return false;

      var results = qr.getResults();
      if (results == null || results.isEmpty()) return false;

      var r0 = results.get(0);
      if (r0 == null || r0.getSeries() == null || r0.getSeries().isEmpty()) return false;

      var values = r0.getSeries().get(0).getValues();
      return values != null && !values.isEmpty(); // 여기만 true/false 판별
    } catch (Exception e) {
      log.warn("[hasResult] query failed url={}, db={}, q={}, err={}",
          s.url(), s.database(), query, e.toString());
      return false;
    }
  }



  private boolean hasError(QueryResult qr) {
    if (qr.getError() != null) return true;
    if (qr.getResults() == null) return false;
    return qr.getResults().stream().anyMatch(r -> r != null && r.getError() != null);
  }


  private String esc(String s) {
    return s == null ? "" : s.replace("'", "\\'");
  }

  private String trimSlash(String url) {
    return (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
  }


}
