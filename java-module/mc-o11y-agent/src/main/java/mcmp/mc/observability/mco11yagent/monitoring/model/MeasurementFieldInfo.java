package mcmp.mc.observability.mco11yagent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MeasurementFieldInfo {

    @ApiModelProperty(value = "influxDB measurement name", example = "cpu")
    @JsonProperty("measurement")
    private String measurement;

    @ApiModelProperty(value = "influxDB field list on measurement", example = "[{\"key\": \"usage_guest\",\"type\": \"float\"}]")
    @JsonProperty("fields")
    private List<FieldInfo> fields = new ArrayList<>();

    @Getter
    @Setter
    public static class FieldInfo {
        @JsonProperty("field_key")
        private String fieldKey;

        @JsonProperty("field_type")
        private String fieldType;
    }
}
