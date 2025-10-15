package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.manager.entity.InfluxDbInfo;
import com.mcmp.o11ymanager.manager.entity.InfluxEntity;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.mapper.Influx.InfluxMapper;
import com.mcmp.o11ymanager.manager.mapper.Influx.QueryMapper;
import com.mcmp.o11ymanager.manager.model.influx.InfluxQl;
import com.mcmp.o11ymanager.manager.repository.InfluxJpaRepository;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfluxDbServiceImpl implements InfluxDbService {

  private final InfluxJpaRepository influxJpaRepository;
  private final InfluxDbInfo influxDbInfo;
  private final InfluxMapper influxMapper;

  private static final String NS_ID = "ns_id";
  private static final String MCI_ID = "mci_id";

  @PostConstruct
  void getInfluxFromYaml() {
    try {
      rawEntities();
    } catch (Exception e) {
      log.error("[INF-BOOTSTRAP] seeding failed", e);
    }
  }

  @Override
  public InfluxDTO get(Long id) {
    InfluxEntity entity =
        influxJpaRepository
            .findById(id)
            .orElseThrow(() -> new IllegalStateException("influx not found: " + id));
    return InfluxDTO.fromEntity(entity);
  }

  private List<InfluxEntity> rawEntities() {
    var list = influxJpaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    if (!list.isEmpty()) {
      return list;
    }

    var servers = influxDbInfo.servers();
    if (servers == null || servers.isEmpty()) {
      return List.of();
    }

    var entities = servers.stream().map(s -> influxMapper.toEntity(s)).toList();

    list = influxJpaRepository.saveAll(entities);
    log.info("[INF-BOOTSTRAP] seeded {} rows from YAML", list.size());
    return list;
  }

  private InfluxDTO toDTO(InfluxEntity e) {
    return InfluxDTO.builder()
        .url(e.getUrl())
        .database(e.getDatabase())
        .username(e.getUsername())
        .password(e.getPassword())
        .retention_policy(e.getRetentionPolicy())
        .uid(e.getUid())
        .build();
  }

  @Override
  public InfluxDTO resolveInfluxDto(String nsId, String mciId) {
    Long influxId = resolveInfluxDb(nsId, mciId);
    InfluxEntity e =
        influxJpaRepository
            .findById(influxId)
            .orElseThrow(
                () -> new IllegalStateException("influx not found: " + influxId));
    return toDTO(e);
  }

  @Override
  public boolean isConnectedDb(InfluxDTO influxDTO) {
    try (var influx =
        InfluxDBFactory.connect(
            influxDTO.getUrl(), influxDTO.getUsername(), influxDTO.getPassword())) {

      Pong pong = influx.ping();
      return pong != null && !"unknown".equalsIgnoreCase(pong.getVersion());
    } catch (Exception e) {
      log.info(
          "[INFLUX CONNECTION TEST] failed url={}, err={}",
          influxDTO.getUrl(),
          e.toString());
      return false;
    }
  }

  // ------------------------------------db
  // list--------------------------------------------------//

  @Override
  public List<InfluxDTO> rawServers() {
    return rawEntities().stream().map(this::toDTO).collect(Collectors.toList());
  }

  // ------------------------------------queryExecutor--------------------------------------------------//
  private Optional<QueryResult> exec(InfluxDTO influxDTO, String query) {
    try (var influx =
        InfluxDBFactory.connect(
            influxDTO.getUrl(), influxDTO.getUsername(), influxDTO.getPassword())) {
      var qr = influx.query(new Query(query, influxDTO.getDatabase()));
      var err = QueryMapper.firstError(qr);
      if (err != null) {
        log.warn(
            "[exec] influx error url={}, db={}, q={}, err={}",
            influxDTO.getUrl(),
            influxDTO.getDatabase(),
            query,
            err);
        return Optional.empty();
      }
      return Optional.ofNullable(qr);
    } catch (Exception e) {
      log.warn(
          "[exec] query failed url={}, db={}, q={}, err={}",
          influxDTO.getUrl(),
          influxDTO.getDatabase(),
          query,
          e.toString());
      return Optional.empty();
    }
  }

  // ------------------------------------getMetric--------------------------------------------------//

  @Override
  public List<MetricDTO> getMetrics(String nsId, String mciId, MetricRequestDTO req) {
    Long influxId = resolveInfluxDb(nsId, mciId);
    InfluxEntity entity =
        influxJpaRepository
            .findById(influxId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "resolved influx not found: " + influxId));

    InfluxDTO s =
        InfluxDTO.builder()
            .url(entity.getUrl())
            .database(entity.getDatabase())
            .username(entity.getUsername())
            .password(entity.getPassword())
            .build();

    String rp = fetchDefaultRp(s);
    String q = InfluxQl.buildQuery(req, rp);

    return exec(s, q).map(QueryMapper::toMetricDTOs).orElse(List.of());
  }

  // ------------------------------------getTag--------------------------------------------------//

  @Override
  public ResBody<List<TagDTO>> getTags() {
    var servers = rawServers();
    var res = new ResBody<List<TagDTO>>();
    if (servers.isEmpty()) {
      res.setData(List.of());
      return res;
    }

    Map<String, Set<String>> acc = new LinkedHashMap<>();

    for (var s : servers) {
      exec(s, "SHOW TAG KEYS")
          .ifPresent(
              qr -> {
                var list = QueryMapper.toTagDTOs(qr);
                for (var dto : list) {
                  var set =
                      acc.computeIfAbsent(
                          dto.getMeasurement(),
                          k -> new LinkedHashSet<>());
                  if (dto.getTags() != null) {
                    set.addAll(dto.getTags());
                  }
                }
              });
    }

    List<TagDTO> out = new ArrayList<>(acc.size());
    for (var e : acc.entrySet()) {
      var dto = new TagDTO();
      dto.setMeasurement(e.getKey());
      dto.setTags(new ArrayList<>(e.getValue()));
      out.add(dto);
    }
    res.setData(out);
    return res;
  }

  // ------------------------------------getField--------------------------------------------------//

  @Override
  public ResBody<List<FieldDTO>> getFields() {
    var servers = rawServers();
    var res = new ResBody<List<FieldDTO>>();
    if (servers.isEmpty()) {
      res.setData(List.of());
      return res;
    }

    Map<String, Map<String, String>> acc = new LinkedHashMap<>();

    for (var s : servers) {
      exec(s, "SHOW FIELD KEYS")
          .ifPresent(
              qr -> {
                var list = QueryMapper.toFieldDTOs(qr);
                for (var dto : list) {
                  var map =
                      acc.computeIfAbsent(
                          dto.getMeasurement(),
                          k -> new LinkedHashMap<>());
                  if (dto.getFields() != null) {
                    for (var f : dto.getFields()) {
                      map.putIfAbsent(f.getKey(), f.getType());
                    }
                  }
                }
              });
    }

    List<FieldDTO> out = new ArrayList<>(acc.size());
    for (var e : acc.entrySet()) {
      var dto = new FieldDTO();
      dto.setMeasurement(e.getKey());
      var fields = new ArrayList<FieldDTO.FieldInfo>();
      for (var fe : e.getValue().entrySet()) {
        var fi = new FieldDTO.FieldInfo();
        fi.setKey(fe.getKey());
        fi.setType(fe.getValue());
        fields.add(fi);
      }
      dto.setFields(fields);
      out.add(dto);
    }
    res.setData(out);
    return res;
  }

  // ------------------------------------resolveInfluxdb--------------------------------------------------//

  @Override
  public Long resolveInfluxDb(String nsId, String mciId) {
    var entities = rawEntities();
    if (entities.isEmpty()) {
      throw new IllegalStateException("influxdb.servers must contain at least 1 server");
    }

    log.info("[INF-RESOLVE] start ns={}, mci={}, servers={}", nsId, mciId, entities.size());

    // DB where (ns_id, mci_id) combination already exists
    for (var e : entities) {
      var s = toDTO(e);
      if (!isConnectedDb(s)) {
        log.info(
            "[INF-RESOLVE] id={}, url={} PING FAIL -> skip ns&mci",
            e.getId(),
            s.getUrl());
        continue;
      }
      if (existsTagCombination(s, nsId, mciId)) {
        log.info(
            "[INF-RESOLVE] found vm combination at id={}, url={}",
            e.getId(),
            s.getUrl());
        return e.getId();
      }
    }

    // Choose the least DB compared to Cardinality
    InfluxEntity best = selectDbByLowestCardinality(entities);
    if (best != null) {
      log.info(
          "[INF-RESOLVE] selected lowest cardinality at id={}, url={}",
          best.getId(),
          best.getUrl());
      return best.getId();
    }

    // Fallback to the first connectable DB
    for (var e : entities) {
      var s = toDTO(e);
      if (isConnectedDb(s)) {
        log.info(
            "[INF-RESOLVE] fallback to reachable id={}, url={}", e.getId(), s.getUrl());
        return e.getId();
      } else {
        log.info(
            "[INF-RESOLVE] id={}, url={} PING FAIL -> skip fallback",
            e.getId(),
            s.getUrl());
      }
    }

    throw new IllegalStateException(
        "no reachable influxdb candidates (ns=" + nsId + ", mci=" + mciId + ")");
  }

  // ------------------------------------Retention
  // Policy--------------------------------------------------//

  @Override
  public String fetchDefaultRp(InfluxDTO influxDTO) {
    String q = "SHOW RETENTION POLICIES ON \"" + influxDTO.getDatabase() + "\"";
    try (var influx =
        InfluxDBFactory.connect(
            influxDTO.getUrl(), influxDTO.getUsername(), influxDTO.getPassword())) {
      var qr = influx.query(new Query(q, influxDTO.getDatabase()));
      if (qr == null || hasError(qr)) {
        return null;
      }

      var results = qr.getResults();
      if (results == null || results.isEmpty()) {
        return null;
      }

      var series = results.get(0).getSeries();
      if (series == null || series.isEmpty()) {
        return null;
      }

      var s0 = series.get(0);
      var cols = s0.getColumns();
      int iName = cols.indexOf("name");
      int iDef = cols.indexOf("default");

      String rpName = null;
      for (var row : s0.getValues()) {
        Object defObj = (iDef >= 0 && iDef < row.size()) ? row.get(iDef) : null;
        boolean isDefault =
            (defObj instanceof Boolean b)
                ? b
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
      log.warn(
          "[fetchDefaultRp] query failed url={}, db={}, err={}",
          influxDTO.getUrl(),
          influxDTO.getDatabase(),
          e.toString());
      return null;
    }
  }

  private InfluxEntity selectDbByLowestCardinality(List<InfluxEntity> entities) {
    InfluxEntity best = null;
    int bestCount = Integer.MAX_VALUE;

    for (var e : entities) {
      var s = toDTO(e);
      if (!isConnectedDb(s)) {
        log.info("[CARDINALITY] id={}, url={} PING FAIL -> skip", e.getId(), s.getUrl());
        continue;
      }
      int count = countMciIdTag(s);
      log.info("[CARDINALITY] id={}, url={}, count={}", e.getId(), s.getUrl(), count);

      if (count < bestCount) {
        bestCount = count;
        best = e;
      }
    }

    return best;
  }

  // ------------------------------------query--------------------------------------------------//

  public boolean existsTagCombination(InfluxDTO influxDTO, String nsId, String mciId) {
    String q =
        "SHOW TAG VALUES ON \""
            + influxDTO.getDatabase()
            + "\" "
            + "WITH KEY=\""
            + NS_ID
            + "\" "
            + "WHERE \""
            + NS_ID
            + "\"='"
            + esc(nsId)
            + "' AND \""
            + MCI_ID
            + "\"='"
            + esc(mciId)
            + "' "
            + "LIMIT 1";
    return hasResult(influxDTO, q);
  }

  public int countMciIdTag(InfluxDTO influxDTO) {
    String q =
        "SHOW TAG VALUES CARDINALITY ON \""
            + influxDTO.getDatabase()
            + "\" "
            + "WITH KEY=\""
            + MCI_ID
            + "\"";
    return resCount(influxDTO, q);
  }

  // ------------------------------------helper--------------------------------------------------//



  private int resCount(InfluxDTO influxDTO, String query) {
    try (var influx =
        InfluxDBFactory.connect(
            influxDTO.getUrl(), influxDTO.getUsername(), influxDTO.getPassword())) {

      QueryResult qr = influx.query(new Query(query, influxDTO.getDatabase()));
      if (qr == null || hasError(qr)) {
        return 0;
      }

      var results = qr.getResults();
      if (results == null || results.isEmpty()) {
        return 0;
      }

      var series = results.get(0).getSeries();
      if (series == null || series.isEmpty()) {
        return 0;
      }

      int total = 0;
      for (var s : series) {
        var values = s.getValues();
        if (values != null && !values.isEmpty()) {
          Object val = values.get(0).get(0);
          if (val != null) {
            try {
              total += (int) Math.floor(Double.parseDouble(val.toString()));
            } catch (NumberFormatException nfe) {
              log.warn("[CARDINALITY] parse error value={}", val);
            }
          }
        }
      }

      return total;

    } catch (Exception e) {
      log.warn(
          "[countMciIdTag] query failed url={}, db={}, q={}, err={}",
          influxDTO.getUrl(),
          influxDTO.getDatabase(),
          query,
          e.toString());
      return 0;
    }
  }


  private boolean hasResult(InfluxDTO influxDTO, String query) {
    try (var influx =
        InfluxDBFactory.connect(
            influxDTO.getUrl(), influxDTO.getUsername(), influxDTO.getPassword())) {
      var qr = influx.query(new Query(query, influxDTO.getDatabase()));
      if (qr == null || hasError(qr)) {
        return false;
      }

      var results = qr.getResults();
      if (results == null || results.isEmpty()) {
        return false;
      }

      var r0 = results.get(0);
      if (r0 == null || r0.getSeries() == null || r0.getSeries().isEmpty()) {
        return false;
      }

      var values = r0.getSeries().get(0).getValues();
      return values != null && !values.isEmpty();
    } catch (Exception e) {
      log.warn(
          "[hasResult] query failed url={}, db={}, q={}, err={}",
          influxDTO.getUrl(),
          influxDTO.getDatabase(),
          query,
          e.toString());
      return false;
    }
  }

  private boolean hasError(QueryResult qr) {
    if (qr.getError() != null) {
      return true;
    }
    if (qr.getResults() == null) {
      return false;
    }
    return qr.getResults().stream().anyMatch(r -> r != null && r.getError() != null);
  }

  private String esc(String s) {
    return s == null ? "" : s.replace("'", "\\'");
  }
}
