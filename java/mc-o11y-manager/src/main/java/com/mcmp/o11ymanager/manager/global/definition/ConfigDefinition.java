package com.mcmp.o11ymanager.manager.global.definition;

public class ConfigDefinition {

    private ConfigDefinition() {
        throw new IllegalStateException("Utility class");
    }

    public static final String CMP_AGENT_ROOT_PATH = "/cmp-agent";

    public static final String HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF = "telegraf";
    public static final String HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT = "fluent-bit";
    public static final String HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG = "telegraf.conf";
    public static final String HOST_CONFIG_NAME_FLUENTBIT_MAIN_CONFIG = "fluent-bit.conf";
    public static final String HOST_CONFIG_NAME_FLUENTBIT_PARSERS_CONFIG = "parsers.conf";
    public static final String HOST_CONFIG_NAME_FLUENTBIT_LOG_LEVEL_LUA = "log-level.lua";
    public static final String HOST_CONFIG_NAME_FLUENTBIT_ADD_TIMESTAMP_LUA = "add-timestamp.lua";
    public static final String HOST_CONFIG_NAME_FLUENTBIT_VARIABLES = "variables";
}
