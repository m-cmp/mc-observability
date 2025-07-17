package com.mcmp.o11ymanager.global.target;

public class Constants {
    public static final String PREFIX_V1 = "/api/o11y";
    public static final String TARGET_PATH = "/monitoring/{nsId}/{mciId}/target/{targetId}";
    public static final String TARGET_ITEM_PATH = "/monitoring/{nsId}/{mciId}/target/{targetId}/item";
    public static final String TARGET_STORAGE_PATH = "/monitoring/{nsId}/{mciId}/target/{targetId}/storage";
    public static final String TARGET_CSP_PATH = "/monitoring/{nsId}/{mciId}/target/{targetId}/csp/{measurement}";
    public static final String INFLUXDB_PATH = "/monitoring/influxdb";
    public static final String PREDICTION_PATH = "/insight/predictions";
    public static final String ANOMALY_PATH = "/insight/anomaly-detection";
}
