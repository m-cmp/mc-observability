package com.mcmp.o11ymanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class MeasurementFieldInfo {

  @Schema(description = "influxDB measurement name", example = "cpu")
  @JsonProperty("measurement")
  private String measurement;


  @Schema(description = "influxDB field list on measurement", example = "[{\"key\": \"usage_guest\",\"type\": \"float\"}]")
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
