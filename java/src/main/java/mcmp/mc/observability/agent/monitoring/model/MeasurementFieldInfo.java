package mcmp.mc.observability.agent.monitoring.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MeasurementFieldInfo {
    @ApiModelProperty(value = "influxDB measurement name", example = "cpu")
    private String measurement;
    @ApiModelProperty(value = "influxDB field list on measurement", example = "[{\"key\": \"usage_guest\",\"type\": \"float\"}]")
    private List<FieldInfo> fields = new ArrayList<>();

    @Setter
    public static class FieldInfo {
        private String fieldKey;
        private String fieldType;

        public String getKey() {
            return fieldKey;
        }

        public String getType() {
            return fieldType;
        }
    }
}
