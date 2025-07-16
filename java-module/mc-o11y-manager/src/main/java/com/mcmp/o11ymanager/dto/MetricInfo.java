package com.mcmp.o11ymanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Builder
public class MetricInfo {


  @Schema(description = "Measurement name")
  @JsonProperty("name")
  private final
  String name;


  @Schema(description = "Column name of values index")
  @JsonProperty("columns")
  private final
  List<String> columns;


  @Schema(description = "Tag list of metric")
  @JsonProperty("tags")
  private final
  Map<String, String> tags;


  @Schema(description = "Sequence by monitoring item")
  @JsonProperty("values")
  private final
  List<List<Object>> values;
}