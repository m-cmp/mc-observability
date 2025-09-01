package com.mcmp.o11ymanager.manager.dto.influx;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;


@Builder
public record MetricDTO(@Schema(description = "Measurement name") @JsonProperty("name") String name,
                        @Schema(description = "Column name of values index") @JsonProperty("columns") List<String> columns,
                        @Schema(description = "Tag list of metric") @JsonProperty("tags") Map<String, String> tags,
                        @Schema(description = "Sequence by monitoring item") @JsonProperty("values") List<List<Object>> values) {

}
