package mcmp.mc.observability.mco11ymanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(description = "Metric information")
public class MetricInfo {

    @Schema(description = "Measurement name")
    @JsonProperty("name")
    private final String name;

    @Schema(description = "Column name of values index")
    @JsonProperty("columns")
    private final List<String> columns;

    @Schema(description = "tag list of metric")
    @JsonProperty("tags")
    private final Map<String, String> tags;

    @Schema(description = "Sequence by monitoring item")
    @JsonProperty("values")
    private final List<List<Object>> values;
}
