package com.mcmp.o11ymanager.manager.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringItemRequestDTO {
    private Long pluginSeq;
    private String pluginConfig;
}