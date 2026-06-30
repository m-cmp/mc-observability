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
    FAILED("There are no agent-related tasks currently in progress."),
    FINISHED("There are no agent-related tasks currently in progress."),
    NOT_INSTALLED("The agent is not installed on this node."),
    IDLE("There are no agent-related tasks currently in progress.");

    private final String message;

    VMAgentTaskStatus(String message) {
        this.message = message;
    }

    /**
     * Whether an agent task is actively running, i.e. a new install/uninstall must be rejected.
     * IDLE/FINISHED/FAILED/NOT_INSTALLED are all settled states from which a new task may start —
     * notably NOT_INSTALLED, which is the resting state of the *other* agent after one agent is
     * installed and must not block installing it.
     */
    public boolean isBusy() {
        return this == PREPARING
                || this == INSTALLING
                || this == UPDATING
                || this == UPDATING_CONFIG
                || this == UNINSTALLING
                || this == RESTARTING;
    }
}
