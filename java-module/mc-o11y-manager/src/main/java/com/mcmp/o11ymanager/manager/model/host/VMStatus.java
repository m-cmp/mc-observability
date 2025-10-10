package com.mcmp.o11ymanager.manager.model.host;

import lombok.Getter;

@Getter
public enum VMStatus {
  RUNNING("The host is running."),
  FAILED("Unable to connect to the host.");


  private final String message;

  VMStatus(String message) {
    this.message = message;
  }
}
