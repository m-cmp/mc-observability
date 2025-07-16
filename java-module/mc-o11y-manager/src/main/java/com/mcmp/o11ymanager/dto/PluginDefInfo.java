package com.mcmp.o11ymanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PluginDefInfo {

  @JsonProperty("seq")
  private Long seq;

  @JsonProperty("name")
  private String name;

  @JsonProperty("plugin_id")
  private String pluginId;

  @JsonProperty("plugin_type")
  private String pluginType;
}
