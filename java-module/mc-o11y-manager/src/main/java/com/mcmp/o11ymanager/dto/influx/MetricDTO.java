package com.mcmp.o11ymanager.dto.influx;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class MetricDTO {

  @Schema(description = "Measurement name")
  @JsonProperty("name") private final String name;


  @Schema(description = "Column name of values index")
  @JsonProperty("columns")
  private final List<String> columns;


  @Schema(description = "Tag list of metric")
  @JsonProperty("tags")
  private final Map<String, String> tags;


  @Schema(description = "Sequence by monitoring item")
  @JsonProperty("values")
  private final List<List<Object>> values;

}
