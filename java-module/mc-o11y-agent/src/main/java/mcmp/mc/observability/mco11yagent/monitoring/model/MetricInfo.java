package mcmp.mc.observability.mco11yagent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MetricInfo {

    @ApiModelProperty(value = "Measurement name")
    @JsonProperty("name")
    private final String name;

    @ApiModelProperty(value = "Column name of values index")
    @JsonProperty("columns")
    private final List<String> columns;

    @ApiModelProperty(value = "tag list of metric")
    @JsonProperty("tags")
    private final Map<String, String> tags;

    @ApiModelProperty(value = "Sequence by monitoring item")
    @JsonProperty("values")
    private final List<List<Object>> values;
}
