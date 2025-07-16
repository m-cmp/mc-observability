package com.mcmp.o11ymanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitoringConfigInfo {

  @JsonProperty(value = "seq")
  private Long seq;

  @JsonProperty("ns_id")
  private String nsId;

  @JsonProperty("mci_id")
  private String mciId;

  @JsonProperty("target_id")
  private String targetId;


  @JsonProperty("name")
  private String name;

  @JsonProperty("state")
  private String state;

  @JsonProperty("plugin_seq")
  private Long pluginSeq;

  @JsonProperty("plugin_name")
  private String pluginName;

  @JsonProperty("plugin_type")
  private String pluginType;

  @JsonProperty("plugin_config")
  private String pluginConfig;
}
