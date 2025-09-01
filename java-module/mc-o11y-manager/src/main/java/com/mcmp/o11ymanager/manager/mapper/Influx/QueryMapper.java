package com.mcmp.o11ymanager.manager.mapper.Influx;

import com.mcmp.o11ymanager.manager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.TagDTO;
import java.util.*;
import org.influxdb.dto.QueryResult;

public class QueryMapper {

  public static List<MetricDTO> toMetricDTOs(QueryResult qr) {
    var out = new ArrayList<MetricDTO>();
    if (qr == null || qr.getResults() == null) {
      return out;
    }

    for (var r : qr.getResults()) {
      if (r == null || r.getSeries() == null) {
        continue;
      }
      for (var s : r.getSeries()) {
        String name = s.getName();
        Map<String, String> tags = s.getTags();
        List<String> columns = s.getColumns();
        List<List<Object>> values = s.getValues();
        out.add(new MetricDTO(name, columns, tags, values));
      }
    }
    return out;
  }

  public static List<TagDTO> toTagDTOs(QueryResult qr) {
    var out = new ArrayList<TagDTO>();
    if (qr == null || qr.getResults() == null || qr.getResults().isEmpty()) {
      return out;
    }
    var res0 = qr.getResults().get(0);
    if (res0 == null || res0.getSeries() == null) {
      return out;
    }

    for (var s : res0.getSeries()) {
      int iKey = s.getColumns().indexOf("tagKey");
      if (iKey < 0) {
        continue;
      }
      var dto = new TagDTO();
      dto.setMeasurement(s.getName());
      var set = new LinkedHashSet<String>();
      for (var row : s.getValues()) {
        if (iKey < row.size() && row.get(iKey) != null) {
          set.add(String.valueOf(row.get(iKey)));
        }
      }
      dto.setTags(new ArrayList<>(set));
      out.add(dto);
    }
    return out;
  }

  public static List<FieldDTO> toFieldDTOs(QueryResult qr) {
    var out = new ArrayList<FieldDTO>();
    if (qr == null || qr.getResults() == null || qr.getResults().isEmpty()) {
      return out;
    }
    var res0 = qr.getResults().get(0);
    if (res0 == null || res0.getSeries() == null) {
      return out;
    }

    for (var s : res0.getSeries()) {
      int iKey = s.getColumns().indexOf("fieldKey");
      int iTyp = s.getColumns().indexOf("fieldType");
      if (iKey < 0 || iTyp < 0) {
        continue;
      }

      var dto = new FieldDTO();
      dto.setMeasurement(s.getName());
      var list = new ArrayList<FieldDTO.FieldInfo>();
      for (var row : s.getValues()) {
        Object k = (iKey < row.size()) ? row.get(iKey) : null;
        Object t = (iTyp < row.size()) ? row.get(iTyp) : null;
        if (k != null && t != null) {
          var fi = new FieldDTO.FieldInfo();
          fi.setKey(String.valueOf(k));
          fi.setType(String.valueOf(t));
          list.add(fi);
        }
      }
      dto.setFields(list);
      out.add(dto);
    }
    return out;
  }

  public static String firstError(QueryResult qr) {
    if (qr == null) {
      return "null QueryResult";
    }
    if (qr.getError() != null) {
      return qr.getError();
    }
    if (qr.getResults() == null) {
      return null;
    }
    return qr.getResults().stream()
        .filter(r -> r != null && r.getError() != null)
        .map(QueryResult.Result::getError)
        .findFirst().orElse(null);
  }

}
