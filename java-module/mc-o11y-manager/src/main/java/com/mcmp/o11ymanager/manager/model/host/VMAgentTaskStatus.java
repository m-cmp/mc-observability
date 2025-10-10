package com.mcmp.o11ymanager.manager.model.host;

import lombok.Getter;

@Getter
public enum VMAgentTaskStatus {
  PREPARING("Agent-related task is being prepared."),
  INSTALLING("Agent installation is in progress."),
  UPDATING("Agent update is in progress."),
  UPDATING_CONFIG("Agent configuration update is in progress."),
  UNINSTALLING("Agent uninstallation is in progress."),
  RESTARTING("Agent restart is in progress."),
  IDLE("There are no agent-related tasks currently in progress.");


  private final String message;

  VMAgentTaskStatus(String message) {
    this.message = message;
  }
}
