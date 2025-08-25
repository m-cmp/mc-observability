package com.mcmp.o11ymanager.dto.influx;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDTO {

  @JsonProperty("measurement")
  private String measurement;

  @JsonProperty("fields")
  private List<FieldInfo> fields;

  @Getter
  @Setter
  public static class FieldInfo {
    @JsonProperty("key")
    private String key;

    @JsonProperty("type")
    private String type;
  }

}
