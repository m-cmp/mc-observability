package com.mcmp.o11ymanager.manager.dto.tumblebug;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Getter
@Setter
public class TumblebugCmd {

  @JsonProperty("command")
  private List<String> command;

  @JsonProperty("user_name")
  private String userName;
}