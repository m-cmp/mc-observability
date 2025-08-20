package com.mcmp.o11ymanager.dto.influx;

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

  private String url;

  private String database;

  private String username;

  private String retention_policy;

  private String password;

}
