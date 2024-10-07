package mcmp.mc.observability.mco11ymanager.common;

public class Constants {
    public static final String PREFIX_V1 = "/api/o11y";
    public static final String TARGET_PATH = "/monitoring/{nsId}/{mciId}/target/{targetId}";
    public static final String TARGET_ITEM_PATH = "/monitoring/{nsId}/{mciId}/target/{targetId}/item";
    public static final String TARGET_STORAGE_PATH = "/monitoring/{nsId}/{mciId}/target/{targetId}/storage";
    public static final String TARGET_CSP_PATH = "/monitoring/{nsId}/{mciId}/target/{targetId}/csp/{measurement}";
    public static final String INFLUXDB_PATH = "/monitoring/influxdb";
    public static final String OPENSEARCH_PATH = "/monitoring/opensearch";
    public static final String MININGDB_PATH = "/monitoring/miningdb";
    public static final String PREDICTION_PATH = "/insight/predictions";
    public static final String ANOMALY_PATH = "/insight/anomaly-detection";
    public static final String TRIGGER_POLICY_PATH = "/trigger/policy";
    public static final String TRIGGER_ALERT_PATH = "/trigger/policy/{policySeq}/alert";
}
