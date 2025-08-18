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



  /**
   * 설계:
   * 1) 모든 연결 가능한 서버에서 (nsId,mciId) 존재 여부 탐색 → 발견 시 그 인덱스 반환
   * 2) 없으면 nsId 기준 cardinality(작을수록 선택) → 그 인덱스 반환
   */
  @Override
  public int resolveInfluxDb(String nsId, String mciId) {
    var servers = influxDbInfo.servers();
    if (servers == null || servers.isEmpty()) {
      throw new IllegalStateException("influxdb.servers must contain at least 1 server");
    }

    log.info("[INF-RESOLVE] start ns={}, mci={}, servers={}", nsId, mciId, servers.size());

    // 1) 존재 여부 우선 탐색 (연결 가능한 서버만)
    for (int i = 0; i < servers.size(); i++) {
      var s = servers.get(i);
      if (!isConnectedDb(s)) {
        log.info("[INF-RESOLVE] idx={}, url={} PING FAIL -> skip", i, s.url());
        continue;
      }
      if (existsTargetSeries(s, nsId, mciId)) {
        log.info("[INF-RESOLVE] found existing series at idx={}, url={}", i, s.url());
        return i; // 0-based
      }
    }

    // 2) 없다면 nsId 기준 cardinality 최소 서버 선택
    int bestIdx = -1;
    long bestCard = Long.MAX_VALUE;

    for (int i = 0; i < servers.size(); i++) {
      var s = servers.get(i);
      if (!isConnectedDb(s)) {
        log.info("[INF-RESOLVE] idx={}, url={} PING FAIL -> skip for cardinality", i, s.url());
        continue;
      }
      long card = countNsCardinality(s, nsId);
      log.info("[INF-RESOLVE] idx={}, url={} nsCardinality={}", i, s.url(), card);
      if (card < bestCard) {
        bestCard = card;
        bestIdx = i;
      }
    }

    if (bestIdx < 0) {
      throw new IllegalStateException("no reachable influxdb candidates (ns=" + nsId + ", mci=" + mciId + ")");
    }

    log.info("[INF-RESOLVE] final choice idx={}, url={}, nsCardinality={}", bestIdx, servers.get(bestIdx).url(), bestCard);
    return bestIdx; // 0-based
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
    // 0-based 인덱스 사용 (일관성)
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

  // ======================== Influx 도우미 ========================

  /**
   * (nsId, mciId) 태그 조합을 가진 시리즈가 존재하는지 빠르게 확인.
   * SHOW SERIES ... LIMIT 1
   */
  private boolean existsTargetSeries(InfluxDbInfo.Server s, String nsId, String mciId) {
    String escNs = esc(nsId);
    String escMci = esc(mciId);

    String q = "SHOW SERIES ON \"" + s.database() + "\" "
        + "WHERE \"" + NS_ID + "\"='" + escNs + "' AND \"" + MCI_ID + "\"='" + escMci + "' "
        + "LIMIT 1";

    try (var influx = org.influxdb.InfluxDBFactory.connect(s.url(), s.username(), s.password())) {
      var qr = influx.query(new org.influxdb.dto.Query(q, s.database()));
      if (qr == null || hasError(qr)) return false;

      var results = qr.getResults();
      if (results == null || results.isEmpty()) return false;
      var r0 = results.get(0);
      if (r0 == null || r0.getSeries() == null || r0.getSeries().isEmpty()) return false;
      var values = r0.getSeries().get(0).getValues();
      return values != null && !values.isEmpty(); // 하나라도 있으면 존재
    } catch (Exception e) {
      log.warn("[existsTargetSeries] query failed url={}, db={}, ns={}, mci={}, err={}",
          s.url(), s.database(), nsId, mciId, e.toString());
      return false;
    }
  }

  /**
   * 같은 nsId를 가진 전체 시리즈 카디널리티.
   * 없거나 에러면 큰 값으로 보지 말고 0으로 처리할 수도 있는데,
   * 운영 안전을 위해 여기선 에러 시 Long.MAX_VALUE 반환하여 선택에서 배제.
   */
  private long countNsCardinality(InfluxDbInfo.Server s, String nsId) {
    String escNs = esc(nsId);
    String q = "SHOW SERIES CARDINALITY ON \"" + s.database() + "\" "
        + "WHERE \"" + NS_ID + "\"='" + escNs + "'";

    try (var influx = org.influxdb.InfluxDBFactory.connect(s.url(), s.username(), s.password())) {
      var qr = influx.query(new org.influxdb.dto.Query(q, s.database()));
      if (qr == null || hasError(qr)) return Long.MAX_VALUE;

      var results = qr.getResults();
      if (results == null || results.isEmpty() || results.get(0) == null) return 0L;
      var seriesList = results.get(0).getSeries();
      if (seriesList == null || seriesList.isEmpty()) return 0L;

      var values = seriesList.get(0).getValues();
      if (values == null || values.isEmpty() || values.get(0) == null) return 0L;

      var row = values.get(0);
      Object val = row.get(row.size() - 1);
      if (val == null) return 0L;

      if (val instanceof Number num) return num.longValue();
      try { return Long.parseLong(val.toString()); }
      catch (NumberFormatException e) { return Long.MAX_VALUE; }
    } catch (Exception e) {
      log.warn("[countNsCardinality] query failed url={}, db={}, ns={}, err={}",
          s.url(), s.database(), nsId, e.toString());
      return Long.MAX_VALUE;
    }
  }

  private boolean hasError(org.influxdb.dto.QueryResult qr) {
    if (qr.getError() != null) return true;
    var results = qr.getResults();
    if (results == null) return false;
    for (var r : results) {
      if (r != null && r.getError() != null) return true;
    }
    return false;
  }

  private String esc(String s) {
    return s == null ? "" : s.replace("'", "\\'");
  }

  private String trimSlash(String url) {
    return (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
  }


}
