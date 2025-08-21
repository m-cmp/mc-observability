package com.mcmp.o11ymanager.dto.influx;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MetricRequestDTO {

  @Schema(description = "측정 항목 이름", example = "cpu", required = true)
  private String measurement;

  @Schema(description = "조회 범위 (단위: s,m,h,d)", example = "1h", required = true)
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



}
