package com.mcmp.o11ymanager.manager.dto.insight.anomaly_detection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetHistoryPathParams {

    private String nsId;
    private String targetId;

    public String getNsId() {
        return nsId;
    }

    public void setNsId(String nsId) {
        this.nsId = nsId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
