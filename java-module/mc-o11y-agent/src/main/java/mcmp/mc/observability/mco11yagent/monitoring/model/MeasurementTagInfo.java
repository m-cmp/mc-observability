package mcmp.mc.observability.mco11yagent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class MeasurementTagInfo {

    @ApiModelProperty(value = "influxDB measurement name", example = "cpu")
    @JsonProperty("measurement")
    private String measurement;

    @ApiModelProperty(value = "influxDB tag list on measurement", example = "[\"cpu\",\"host\"]")
    @JsonProperty("tags")
    private List<String> tags;
}
