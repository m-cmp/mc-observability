package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.config.InfluxDbInfo;
import com.mcmp.o11ymanager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.global.annotation.Base64Encode;
import com.mcmp.o11ymanager.global.target.ResBody;
import com.mcmp.o11ymanager.repository.TargetJpaRepository;
import com.mcmp.o11ymanager.service.interfaces.InfluxDbService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfluxDbServiceImpl implements InfluxDbService {


  private final InfluxDbInfo influxDbInfo;

  private static final String NS_ID = "ns_id";
  private static final String MCI_ID = "mci_id";


  private List<InfluxDbInfo.Server> rawServers() {
    var servers = rawServers();
    if (servers == null) return List.of();

    return servers.stream()
        .map(s -> new InfluxDbInfo.Server(
            s.url(),
            s.database(),
            s.username(),
            s.password()
        ))
        .toList();
  }



  @Override
  public ResBody<List<TagDTO>> getTags() {
    var servers = rawServers();
    ResBody<List<TagDTO>> res = new ResBody<>();
    if (servers == null || servers.isEmpty()) {
      res.setData(List.of());
      return res;
    }


    Map<String, Set<String>> tagsByMeasurement = new java.util.LinkedHashMap<>();

    for (var s : servers) {
      try (var influx = org.influxdb.InfluxDBFactory.connect(s.url(), s.username(), s.password())) {
        var qr = influx.query(new org.influxdb.dto.Query("SHOW TAG KEYS", s.database()));
        if (qr == null || hasError(qr)) continue;

        var results = qr.getResults();
        if (results == null || results.isEmpty()) continue;

        var seriesList = results.get(0).getSeries();
        if (seriesList == null || seriesList.isEmpty()) continue;

        for (var series : seriesList) {
          String measurement = series.getName(); // measurement 이름
          var cols = series.getColumns();
          int iKey = cols.indexOf("tagKey");
          if (iKey < 0) continue;

          var set = tagsByMeasurement.computeIfAbsent(measurement, k -> new java.util.LinkedHashSet<>());
          for (var row : series.getValues()) {
            Object v = (iKey < row.size()) ? row.get(iKey) : null;
            if (v != null) set.add(String.valueOf(v));
          }
        }
      } catch (Exception e) {
        log.warn("[getTags] query failed url={}, db={}, err={}", s.url(), s.database(), e.toString());
      }
    }


    List<TagDTO> out = new java.util.ArrayList<>(tagsByMeasurement.size());
    for (var e : tagsByMeasurement.entrySet()) {
      TagDTO dto = new TagDTO();
      dto.setMeasurement(e.getKey());
      dto.setTags(new java.util.ArrayList<>(e.getValue()));
      out.add(dto);
    }
    res.setData(out);
    return res;
  }



  @Override
  public ResBody<List<FieldDTO>> getFields() {
    var servers = influxDbInfo.servers();
    ResBody<List<FieldDTO>> res = new ResBody<>();
    if (servers == null || servers.isEmpty()) {
      res.setData(List.of());
      return res;
    }

    Map<String, java.util.Map<String, String>> fieldsByMeasurement = new java.util.LinkedHashMap<>();

    for (var s : servers) {
      try (var influx = org.influxdb.InfluxDBFactory.connect(s.url(), s.username(), s.password())) {
        var qr = influx.query(new org.influxdb.dto.Query("SHOW FIELD KEYS", s.database()));
        if (qr == null || hasError(qr)) continue;

        var results = qr.getResults();
        if (results == null || results.isEmpty()) continue;

        var seriesList = results.get(0).getSeries();
        if (seriesList == null || seriesList.isEmpty()) continue;

        for (var series : seriesList) {
          String measurement = series.getName();
          var cols = series.getColumns();
          int iKey = cols.indexOf("fieldKey");
          int iTyp = cols.indexOf("fieldType");
          if (iKey < 0 || iTyp < 0) continue;

          var map = fieldsByMeasurement.computeIfAbsent(measurement, k -> new java.util.LinkedHashMap<>());
          for (var row : series.getValues()) {
            Object k = (iKey < row.size()) ? row.get(iKey) : null;
            Object t = (iTyp < row.size()) ? row.get(iTyp) : null;
            if (k != null && t != null) {
              map.putIfAbsent(String.valueOf(k), String.valueOf(t));
            }
          }
        }
      } catch (Exception e) {
        log.warn("[getFields] query failed url={}, db={}, err={}", s.url(), s.database(), e.toString());
      }
    }

    List<FieldDTO> out = new java.util.ArrayList<>(fieldsByMeasurement.size());
    for (var e : fieldsByMeasurement.entrySet()) {
      FieldDTO dto = new FieldDTO();
      dto.setMeasurement(e.getKey());

      List<FieldDTO.FieldInfo> fields = new java.util.ArrayList<>();
      for (var fe : e.getValue().entrySet()) {
        FieldDTO.FieldInfo fi = new FieldDTO.FieldInfo();
        fi.setFieldKey(fe.getKey());
        fi.setFieldType(fe.getValue()); // float|integer|string|boolean
        fields.add(fi);
      }
      dto.setFields(fields);
      out.add(dto);
    }
    res.setData(out);
    return res;
  }





  @Override
  public int resolveInfluxDb(String nsId, String mciId) {
    var servers = rawServers();
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
  @Base64Encode
  public ResBody<List<InfluxDTO>> getList() {

    var servers = rawServers();
    List<InfluxDTO> list = new java.util.ArrayList<>();


    if (servers != null) {
      for (int i = 0; i < servers.size(); i++) {
        var s = servers.get(i);

        InfluxDTO dto = new InfluxDTO();
        dto.setSeq(i);
        dto.setUrl(s.url());
        dto.setDatabase(s.database());
        dto.setUsername(s.username());
        dto.setPassword(s.password());
        dto.setRetention_policy(fetchDefaultRp(s));

        list.add(dto);
      }
    }


    ResBody<List<InfluxDTO>> res = new ResBody<>();
    res.setData(list);

    return res;
  }

  @Override
  public String fetchDefaultRp(InfluxDbInfo.Server s) {
    String q = "SHOW RETENTION POLICIES ON \"" + s.database() + "\"";
    try (var influx = org.influxdb.InfluxDBFactory.connect(s.url(), s.username(), s.password())) {
      var qr = influx.query(new org.influxdb.dto.Query(q, s.database()));
      if (qr == null || hasError(qr)) return null;

      var results = qr.getResults();
      if (results == null || results.isEmpty()) return null;

      var series = results.get(0).getSeries();
      if (series == null || series.isEmpty()) return null;

      var s0   = series.get(0);
      var cols = s0.getColumns();
      int iName = cols.indexOf("name");
      int iDef  = cols.indexOf("default");

      String rpName = null;
      for (var row : s0.getValues()) {
        Object defObj = (iDef >= 0 && iDef < row.size()) ? row.get(iDef) : null;
        boolean isDefault = (defObj instanceof Boolean b) ? b
            : defObj != null && Boolean.parseBoolean(String.valueOf(defObj));
        if (isDefault) {
          rpName = String.valueOf(row.get(iName));
          break;
        }
      }

      if (rpName == null && !s0.getValues().isEmpty()) {
        rpName = String.valueOf(s0.getValues().get(0).get(iName));
      }
      return rpName;
    } catch (Exception e) {
      log.warn("[fetchDefaultRp] query failed url={}, db={}, err={}", s.url(), s.database(), e.toString());
      return null;
    }
  }



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
  public InfluxDbInfo.Server resolveByNo(int influxNo) {
    var servers = rawServers();
    if (servers == null) {
      throw new IllegalStateException("influxdb.servers not configured");
    }

    if (influxNo < 0 || influxNo >= servers.size()) {
      throw new IllegalStateException("invalid influx seq (0-based): " + influxNo);
    }
    return servers.get(influxNo);
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



}
