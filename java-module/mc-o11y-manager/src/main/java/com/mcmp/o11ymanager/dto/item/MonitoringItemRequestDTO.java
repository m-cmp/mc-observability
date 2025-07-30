package com.mcmp.o11ymanager.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringItemRequestDTO {
    private String name;
    private Long pluginSeq;
    private String pluginConfig;
}