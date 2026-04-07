package com.mcmp.o11ymanager.manager.dto.influx;

/** Lightweight identifier for a VM that has metric data in InfluxDB. */
public record VmRef(String nsId, String mciId, String vmId) {}
