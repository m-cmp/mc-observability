package com.mcmp.o11ymanager.dto.tumblebug;

import lombok.Data;

@Data
public class SshKey {
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
