package mcmp.mc.observability.mco11yagent.monitoring.common;

public class Constants {
    public static final String MONITORING_URI = "/api/o11y/monitoring";
    public static final String EMPTY_HOST = "0.0.0.0";
    public static final String CONFIG_ROOT_PATH = "/etc/mc-observability-agent";
    public static final String BIN_ROOT_PATH = "/usr/bin/mc-observability-agent";
    public static final String COLLECTOR_PATH = BIN_ROOT_PATH + "/mc-observability-agent-collector";
    public static final String COLLECTOR_CONFIG_PATH = CONFIG_ROOT_PATH + "/mc-observability-agent-collector.conf";
    public static final String COLLECTOR_CONFIG_DIR_PATH = CONFIG_ROOT_PATH + "/conf";

    public static final String PROPERTY_NS_ID = "NS_ID";
    public static final String PROPERTY_MCI_ID = "MCI_ID";
    public static final String PROPERTY_TARGET_ID = "TARGET_ID";
}