package com.mcmp.o11ymanager.dto.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDefDTO {
    private Long seq;
    private String name;
    private String pluginId;
    private String pluginType;
}