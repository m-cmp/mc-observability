package mcmp.mc.observability.agent.common;

public class Constants {
    public static final String PREFIX_V1 = "/api/v1/agent";
    public static final String EMPTY_HOST = "0.0.0.0";
    public static final String AGENT_UUID_PATH = "./uuid";
    public static final String COLLECTOR_PATH = "./telegraf";
    public static final String COLLECTOR_CONFIG_PATH = "./telegraf.conf";
    public static final String COLLECTOR_CONFIG_DIR_PATH = "./conf";
    public static final Integer INTERVAL_MIN = 10;
    public static final String UUID_PROPERTY_KEY = "m-cmp-agent.uuid";
}
