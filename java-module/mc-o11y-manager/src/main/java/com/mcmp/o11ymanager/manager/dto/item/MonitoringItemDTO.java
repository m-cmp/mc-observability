package com.mcmp.o11ymanager.manager.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringItemDTO {
    private Long seq;
    private String nsId;
    private String mciId;
    private String targetId;
    private String name;
    private String state;
    private Long pluginSeq;
    private String pluginName;
    private String pluginType;
    private String pluginConfig;
}
