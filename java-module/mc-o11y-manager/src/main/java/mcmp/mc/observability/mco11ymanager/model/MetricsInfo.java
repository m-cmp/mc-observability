package mcmp.mc.observability.mco11ymanager.model;

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
    @Schema(required = true, example = "1")
    @JsonProperty("influx_db_seq")
    private Long influxDBSeq;

    @Schema(required = true, example = "cpu")
    @JsonProperty("measurement")
    private String measurement;

    @Schema(required = true, example = "1h", description = "timeunit s,m,h,d(second, minute, hour, day)")
    @JsonProperty("range")
    private String range;

    @Schema(example = "1h", description = "timeunit s,m,h,d(second, minute, hour, day)")
    @JsonProperty("group_time")
    private String groupTime;

    @Schema(example = "[\"uuid\",\"cpu\"]")
    @JsonProperty("group_by")
    private List<String> groupBy;

    @Schema(example = "10")
    @JsonProperty("limit")
    private Long limit;

    @Schema(example = "[{\"function\":\"mean\",\"field\":\"usage_idle\"}]")
    @JsonProperty("fields")
    private List<FieldInfo> fields;

    @Schema(example = "[{\"key\":\"cpu\",\"value\":\"cpu-total\"}]")
    @JsonProperty("conditions")
    private List<ConditionInfo> conditions;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldInfo {
        @JsonProperty("function")
        private String function;

        @JsonProperty("field")
        private String field;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConditionInfo {
        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;
    }

    private String getFieldQuery() {
        if( fields == null || fields.isEmpty() ) return ", *";
        StringBuilder sb = new StringBuilder();
        for( FieldInfo field : fields ) {
            sb.append(", ");
            if( field.getFunction() == null ) sb.append(field.getField());
            else {
                sb.append(field.getFunction())
                        .append("(")
                        .append(field.getField())
                        .append(")")
                        .append(" as ")
                        .append(field.getField());
            }
        }
        return sb.toString();
    }

    private String getConditionQuery() {
        if( conditions == null || conditions.isEmpty() ) return "";
        StringBuilder sb = new StringBuilder();
        for( ConditionInfo conditionInfo : conditions ) {
            sb.append(" and ")
                    .append(conditionInfo.getKey())
                    .append("=")
                    .append("'")
                    .append(conditionInfo.getValue())
                    .append("'");
        }
        return sb.toString();
    }

    private String getGroupByQuery() {
        if( groupTime == null && (groupBy == null || groupBy.isEmpty()) ) return "";
        StringBuilder sb = new StringBuilder("group by ");

        if( groupTime != null ) sb.append(" time(").append(groupTime).append(")");
        if( groupBy != null && !groupBy.isEmpty() ) {
            if( !sb.toString().equals("group by ") ) sb.append(",");
            sb.append(String.join(",", groupBy));
        }

        return sb.toString();
    }

    private String getLimitQuery() {
        return (getLimit() != null && getLimit() > 0? "limit " + getLimit(): "");
    }

    @Schema(hidden = true)
    public boolean isVaild() {
        if( fields !=  null && !fields.isEmpty() ) {
            boolean isSimple = false;
            boolean isFunction = false;

            for( FieldInfo info : fields) {
                if( info.getFunction() != null ) isFunction = true;
                else isSimple = true;
                if( info.getField() == null ) return false;
            }
            if( isSimple && isFunction ) return false;
        }

        if( conditions != null && !conditions.isEmpty() ) {
            for( ConditionInfo info : conditions ) {
                if( info.getKey().isEmpty() || info.getValue().isEmpty() ) return false;
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
}
