package com.mcmp.o11ymanager.manager.enums;

import lombok.Getter;

@Getter
public enum SemaphoreInstallMethod {
  INSTALL("install"),
  UNINSTALL("uninstall"),
  UPDATE("update"),
  RESTART("restart"),
  CONFIG_UPDATE("config_update"),
  ROLLBACK_CONFIG("rollback_config");

  private final String value;

  SemaphoreInstallMethod(String value) {
    this.value = value;
  }
}
