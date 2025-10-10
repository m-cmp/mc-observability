package com.mcmp.o11ymanager.manager.dto.influx;

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

    @Schema(description = "Measurement name", example = "cpu", required = true)
    private String measurement;

    @Schema(description = "Query range (unit: s, m, h, d)", example = "1h", required = true)
    private String range;

    @Schema(description = "Grouping time unit (unit: s, m, h, d)", example = "1h")
    @JsonProperty("group_time")
    private String groupTime;

    @Schema(description = "List of fields to group by", example = "[\"uuid\",\"cpu\"]")
    @JsonProperty("group_by")
    private List<String> groupBy;

    @Schema(description = "Result limit count", example = "10")
    @JsonProperty("limit")
    private Long limit;

    @Schema(
            description = "List of fields to query",
            example = "[{\"function\":\"mean\",\"field\":\"usage_idle\"}]")
    @JsonProperty("fields")
    private List<FieldInfo> fields;

    @Schema(
            description = "List of condition filters",
            example = "[{\"key\":\"cpu\",\"value\":\"cpu-total\"}]")
    @JsonProperty("conditions")
    private List<ConditionInfo> conditions;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldInfo {

        @JsonProperty("function")
        @Schema(description = "Aggregation function (e.g., mean, max, etc.)", example = "mean")
        private String function;

        @JsonProperty("field")
        @Schema(description = "Field name", example = "usage_idle")
        private String field;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConditionInfo {

        @JsonProperty("key")
        @Schema(description = "Condition key", example = "cpu")
        private String key;

        @JsonProperty("value")
        @Schema(description = "Condition value", example = "cpu-total")
        private String value;
    }
}
