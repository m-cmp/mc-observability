package com.mcmp.o11ymanager.manager.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringItemUpdateDTO {
    private Long seq;
    private String pluginConfig;
}
