package mcmp.mc.observability.agent.monitoring.model;

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
    private String measurement;
    @ApiModelProperty(value = "influxDB tag list on measurement", example = "[\"cpu\",\"host\"]")
    private List<String> tags;
}
