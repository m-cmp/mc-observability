package com.mcmp.o11ymanager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcmp.o11ymanager.global.annotation.Base64DecodeField;
import com.mcmp.o11ymanager.global.annotation.Base64EncodeField;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TargetInfo {

  @JsonProperty("ns_id")
  private String nsId;

  @JsonProperty("mci_id")
  private String mciId;

  @JsonProperty("id")
  private String id;

  @JsonProperty("name")
  private String name;

  @Base64EncodeField
  @Base64DecodeField
  @JsonProperty("description")
  private String description;

  @JsonProperty("state")
  private String state;
}
