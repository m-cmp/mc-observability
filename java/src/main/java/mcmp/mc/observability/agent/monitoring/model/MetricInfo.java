package mcmp.mc.observability.agent.monitoring.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MetricInfo {
    @ApiModelProperty(value = "Measurement name")
    private final String name;
    @ApiModelProperty(value = "Column name of values index")
    private final List<String> columns;
    @ApiModelProperty(value = "tag list of metric")
    private final Map<String, String> tags;
    @ApiModelProperty(value = "Sequence by monitoring item")
    private final List<List<Object>> values;
}
