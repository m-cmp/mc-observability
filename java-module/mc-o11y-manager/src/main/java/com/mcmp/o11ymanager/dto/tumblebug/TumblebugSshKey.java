package com.mcmp.o11ymanager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TumblebugSshKey {
  private String resourceType;
  private String id;
  private String uid;
  private String cspResourceName;
  private String cspResourceId;
  private String name;
  private String connectionName;
  private String description;

  private String publicKey;
  private String privateKey;
}
