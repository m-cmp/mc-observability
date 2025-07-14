package com.mcmp.o11ymanager.enums;

import lombok.Getter;

@Getter
public enum AgentAction {

    HOST_CREATED("Host created"),
    HOST_UPDATED("Host updated"),
    HOST_DELETED("Host deleted"),

    VSPHERE_HOST_CREATED("vSphere Host created"),
    VSPHERE_HOST_UPDATED("vSphere Host updated"),
    VSPHERE_HOST_DELETED("vSphere Host deleted"),

    MONITORING_AGENT_INSTALL_STARTED("Monitoring Agent Install Started"),
    MONITORING_AGENT_INSTALL_FINISHED("Monitoring Agent Install Finished"),
    MONITORING_AGENT_INSTALL_FAILED("Monitoring Agent Install Failed"),
    MONITORING_AGENT_UPDATE_STARTED("Monitoring Agent Update Started"),
    MONITORING_AGENT_UPDATE_FINISHED("Monitoring Agent Update Finished"),
    MONITORING_AGENT_UPDATE_FAILED("Monitoring Agent Update Failed"),
    MONITORING_AGENT_UNINSTALL_STARTED("Monitoring Agent Uninstall Started"),
    MONITORING_AGENT_UNINSTALL_FINISHED("Monitoring Agent Uninstall Finished"),
    MONITORING_AGENT_UNINSTALL_FAILED("Monitoring Agent Uninstall Failed"),
    MONITORING_AGENT_CONFIG_UPDATE_STARTED("Monitoring Agent Config Update Started"),
    MONITORING_AGENT_CONFIG_UPDATE_FINISHED("Monitoring Agent Config Update Finished"),
    MONITORING_AGENT_CONFIG_UPDATE_FAILED("Monitoring Agent Config Update Failed"),
    MONITORING_AGENT_CONFIG_ROLLBACK_STARTED("Monitoring Agent Config Rollback Started"),
    MONITORING_AGENT_CONFIG_ROLLBACK_FINISHED("Monitoring Agent Config Rollback Finished"),
    MONITORING_AGENT_CONFIG_ROLLBACK_FAILED("Monitoring Agent Config Rollback Failed"),

    LOG_AGENT_INSTALL_STARTED("Log Agent Install Started"),
    LOG_AGENT_INSTALL_FINISHED("Log Agent Install Finished"),
    LOG_AGENT_INSTALL_FAILED("Log Agent Install Failed"),
    LOG_AGENT_UPDATE_STARTED("Log Agent Update Started"),
    LOG_AGENT_UPDATE_FINISHED("Log Agent Update Finished"),
    LOG_AGENT_UPDATE_FAILED("Log Agent Update Failed"),
    LOG_AGENT_UNINSTALL_STARTED("Log Agent Uninstall Started"),
    LOG_AGENT_UNINSTALL_FINISHED("Log Agent Uninstall Finished"),
    LOG_AGENT_UNINSTALL_FAILED("Log Agent Uninstall Failed"),
    LOG_AGENT_CONFIG_UPDATE_STARTED("Log Agent Config Update Started"),
    LOG_AGENT_CONFIG_UPDATE_FINISHED("Log Agent Config Update Finished"),
    LOG_AGENT_CONFIG_UPDATE_FAILED("Log Agent Config Update Failed"),
    LOG_AGENT_CONFIG_ROLLBACK_STARTED("Log Agent Config Rollback Started"),
    LOG_AGENT_CONFIG_ROLLBACK_FINISHED("Log Agent Config Rollback Finished"),
    LOG_AGENT_CONFIG_ROLLBACK_FAILED("Log Agent Config Rollback Failed"),

    ENABLE_TELEGRAF("Fluent-Bit Enabled"),
    ENABLE_FLUENT_BIT("Fluent-Bit Enabled"),
    DISABLE_TELEGRAF("Telegraf Disabled"),
    DISABLE_FLUENT_BIT("Fluent-Bit Disabled"),
    RESTART_TELEGRAF("Telegraf Restarted"),
    RESTART_FLUENT_BIT("Fluent-Bit Restarted");

    private final String message;

    AgentAction(String message) {
        this.message = message;
    }
}
