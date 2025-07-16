package com.mcmp.o11ymanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
public class MetricsInfo {

  @Schema(description = "Influx DB 시퀀스", example = "1", required = true)
  @JsonProperty("influx_db_seq")
  private Long influxDBSeq;

  @Schema(description = "측정 항목 이름", example = "cpu", required = true)
  @JsonProperty("measurement")
  private String measurement;

  @Schema(description = "조회 범위 (단위: s,m,h,d)", example = "1h", required = true)
  @JsonProperty("range")
  private String range;

  @Schema(description = "그룹핑 시간 단위 (단위: s,m,h,d)", example = "1h")
  @JsonProperty("group_time")
  private String groupTime;

  @Schema(description = "Group by 대상 필드 목록", example = "[\"uuid\",\"cpu\"]")
  @JsonProperty("group_by")
  private List<String> groupBy;

  @Schema(description = "결과 제한 개수", example = "10")
  @JsonProperty("limit")
  private Long limit;

  @Schema(description = "조회할 필드 목록", example = "[{\"function\":\"mean\",\"field\":\"usage_idle\"}]")
  @JsonProperty("fields")
  private List<FieldInfo> fields;

  @Schema(description = "조건 필터 목록", example = "[{\"key\":\"cpu\",\"value\":\"cpu-total\"}]")
  @JsonProperty("conditions")
  private List<ConditionInfo> conditions;

  @Getter
  @Setter
  @NoArgsConstructor
  public static class FieldInfo {

    @JsonProperty("function")
    @Schema(description = "집계 함수 (예: mean, max 등)", example = "mean")
    private String function;

    @JsonProperty("field")
    @Schema(description = "필드 이름", example = "usage_idle")
    private String field;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class ConditionInfo {

    @JsonProperty("key")
    @Schema(description = "조건 키", example = "cpu")
    private String key;

    @JsonProperty("value")
    @Schema(description = "조건 값", example = "cpu-total")
    private String value;
  }

  @Schema(hidden = true)
  public boolean isVaild() {
    if (fields != null && !fields.isEmpty()) {
      boolean isSimple = false;
      boolean isFunction = false;

      for (FieldInfo info : fields) {
          if (info.getFunction() != null) {
              isFunction = true;
          } else {
              isSimple = true;
          }
          if (info.getField() == null) {
              return false;
          }
      }
        if (isSimple && isFunction) {
            return false;
        }
    }

    if (conditions != null && !conditions.isEmpty()) {
      for (ConditionInfo info : conditions) {
          if (info.getKey().isEmpty() || info.getValue().isEmpty()) {
              return false;
          }
      }
    }
    return true;
  }

  @Schema(hidden = true)
  public String getQuery() {
    String query = "select time as timestamp @FIELD from @MEASUREMENT where time > now() - @RANGE @CONDITION @GROUP_BY order by time desc @LIMIT";
    query = query.replaceAll("@FIELD", getFieldQuery())
        .replaceAll("@MEASUREMENT", getMeasurement())
        .replaceAll("@RANGE", getRange())
        .replaceAll("@CONDITION", getConditionQuery())
        .replaceAll("@GROUP_BY", getGroupByQuery())
        .replaceAll("@LIMIT", getLimitQuery());
    return query;
  }

  private String getFieldQuery() {
      if (fields == null || fields.isEmpty()) {
          return ", *";
      }
    StringBuilder sb = new StringBuilder();
    for (FieldInfo field : fields) {
      sb.append(", ");
      if (field.getFunction() == null) {
        sb.append(field.getField());
      } else {
        sb.append(field.getFunction())
            .append("(")
            .append(field.getField())
            .append(") as ")
            .append(field.getField());
      }
    }
    return sb.toString();
  }

  private String getConditionQuery() {
      if (conditions == null || conditions.isEmpty()) {
          return "";
      }
    StringBuilder sb = new StringBuilder();
    for (ConditionInfo conditionInfo : conditions) {
      sb.append(" and ")
          .append(conditionInfo.getKey())
          .append("='")
          .append(conditionInfo.getValue())
          .append("'");
    }
    return sb.toString();
  }

  private String getGroupByQuery() {
      if (groupTime == null && (groupBy == null || groupBy.isEmpty())) {
          return "";
      }
    StringBuilder sb = new StringBuilder("group by ");

      if (groupTime != null) {
          sb.append(" time(").append(groupTime).append(")");
      }
    if (groupBy != null && !groupBy.isEmpty()) {
        if (!sb.toString().equals("group by ")) {
            sb.append(",");
        }
      sb.append(String.join(",", groupBy));
    }

    return sb.toString();
  }

  private String getLimitQuery() {
    return (getLimit() != null && getLimit() > 0 ? "limit " + getLimit() : "");
  }
}
