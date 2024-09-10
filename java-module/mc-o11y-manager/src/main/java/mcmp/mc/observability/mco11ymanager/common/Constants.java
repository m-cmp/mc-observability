package mcmp.mc.observability.mco11ymanager.common;

public class Constants {
    public static final String PREFIX_V1 = "/api/o11y";
    public static final String TARGET_PATH = "/monitoring/{nsId}/target/{targetId}";
    public static final String TARGET_ITEM_PATH = "/monitoring/{nsId}/target/{targetId}/item";
    public static final String TARGET_STORAGE_PATH = "/monitoring/{nsId}/target/{targetId}/storage";
    public static final String TRIGGER = PREFIX_V1 + "/trigger/policy";
    public static final String TRIGGER_ALERT = "/{policySeq}/alert";
    public static final String INFLUXDB_PATH = "/monitoring/influxdb";
}
