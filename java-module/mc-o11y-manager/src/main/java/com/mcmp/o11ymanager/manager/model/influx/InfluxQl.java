package com.mcmp.o11ymanager.manager.model.influx;

import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO.FieldInfo;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.util.StringUtils;

public class InfluxQl {

  // -----------------------------------generate query--------------------------------------------------//
  public static String buildQuery(MetricRequestDTO r, String retentionPolicy) {
    if (!StringUtils.hasText(r.getMeasurement()))
      throw new IllegalArgumentException("measurement는 필수입니다.");
    if (!StringUtils.hasText(r.getRange()) || !r.getRange().matches("\\d+[smhd]"))
      throw new IllegalArgumentException("The range must be in the format 10s/5m/1h/2d.");

    String select = "select time as timestamp" + projection(r.getFields());
    String from   = " from " + qualifiedMeasurement(retentionPolicy, r.getMeasurement());
    String where  = " where time > now() - " + r.getRange() + conditions(r.getConditions());
    String group  = groupBy(r.getGroupTime(), r.getGroupBy());
    String order  = " order by time desc";
    String limit  = (r.getLimit() != null && r.getLimit() > 0) ? " limit " + r.getLimit() : "";

    return (select + from + where + group + order + limit).trim();
  }


  // ------------------------------------select query--------------------------------------------------//
  private static String projection(List<FieldInfo> fields) {
    if (fields == null || fields.isEmpty()) return ", *";
    var j = new StringJoiner(", ", ", ", "");
    boolean anyFunc=false, anySimple=false;

    for (var f : fields) {
      String fn = f.getFunction();
      String fd = f.getField();
      if (!StringUtils.hasText(fd))
        throw new IllegalArgumentException("fields[].fields are required");
      if (StringUtils.hasText(fn)) { anyFunc = true; j.add(fn + "(" + fd + ") as " + fd); }
      else { anySimple = true; j.add(fd); }
    }
    if (anyFunc && anySimple)
      throw new IllegalArgumentException("fields: Simple fields and aggregate functions are not mixed");
    return j.toString();
  }


  // ------------------------------------where query--------------------------------------------------//
  private static String conditions(List<MetricRequestDTO.ConditionInfo> conds) {
    if (conds == null || conds.isEmpty()) return "";
    var sb = new StringBuilder();
    for (var c : conds) {
      if (!StringUtils.hasText(c.getKey()) || !StringUtils.hasText(c.getValue()))
        throw new IllegalArgumentException("The key/value of conditions cannot be empty.");
      sb.append(" and ")
          .append("\"").append(escapeIdent(c.getKey())).append("\"")
          .append("='").append(escapeString(c.getValue())).append("'");
    }
    return sb.toString();
  }

  // ------------------------------------groupby query--------------------------------------------------//
  private static String groupBy(String groupTime, List<String> groupBy) {
    boolean hasTime  = StringUtils.hasText(groupTime);
    boolean hasGroup = groupBy != null && !groupBy.isEmpty();
    if (!hasTime && !hasGroup) return "";
    var j = new StringJoiner(", ", " group by ", "");
    if (hasTime) j.add("time(" + groupTime + ")");
    if (hasGroup) j.add(String.join(",", groupBy));
    return j.toString();
  }

  private static String qualifiedMeasurement(String rp, String measurement) {
    return StringUtils.hasText(rp) ? rp + "." + measurement : measurement;
  }

  private static String escapeIdent(String s) { return s.replaceAll("[^A-Za-z0-9_\\-\\.]", ""); }
  private static String escapeString(String s) { return s.replace("'", "\\'"); }


}
