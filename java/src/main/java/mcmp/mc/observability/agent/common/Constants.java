package mcmp.mc.observability.agent.common;

public class Constants {
    public static final String PREFIX_V1 = "/api/o11y";
    public static final String MONITORING_URI = PREFIX_V1 + "/monitoring";
    public static final String TRIGGER_URI = PREFIX_V1 + "/trigger";
    public static final String ALERT_URI = PREFIX_V1 + "/alert";
    public static final String EMPTY_HOST = "0.0.0.0";
    public static final String CONFIG_ROOT_PATH = "/etc/mc-agent";
    public static final String BIN_ROOT_PATH = "/usr/bin/mc-agent";
    public static final String AGENT_UUID_PATH = CONFIG_ROOT_PATH + "/uuid";
    public static final String COLLECTOR_PATH = BIN_ROOT_PATH + "/mc-agent-collector";
    public static final String COLLECTOR_CONFIG_PATH = CONFIG_ROOT_PATH + "/mc-agent-collector.conf";
    public static final String COLLECTOR_CONFIG_DIR_PATH = CONFIG_ROOT_PATH + "/conf";
    public static final Integer INTERVAL_MIN = 10;
    public static final String UUID_PROPERTY_KEY = "m-cmp-agent.uuid";
    public static final String EXTENSION_PROPERTY_KEY = "m-cmp-agent.ex";
}
