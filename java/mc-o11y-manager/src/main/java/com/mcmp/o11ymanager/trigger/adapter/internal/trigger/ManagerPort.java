package com.mcmp.o11ymanager.trigger.adapter.internal.trigger;

public interface ManagerPort {

    // infraId narrows a node target to one infra (node IDs repeat across infras); may be null.
    String getInfluxUid(String nsId, String vmScope, String nodeId, String infraId);
}
