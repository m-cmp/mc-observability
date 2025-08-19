package com.mcmp.o11ymanager.dto.influx;

import com.mcmp.o11ymanager.global.annotation.Base64DecodeField;
import com.mcmp.o11ymanager.global.annotation.Base64EncodeField;
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
public class InfluxDTO {

  private int seq;

  private String url;

  private String database;

  private String username;

  private String retention_policy;

  @Base64DecodeField
  @Base64EncodeField
  private String password;

}
