package com.mcmp.o11ymanager.manager.dto.plugin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDefDTO {
    @Schema(description = "Plugin sequence", example = "0")
    private Long seq;

    @Schema(description = "Plugin name", example = "string")
    private String name;

    @Schema(description = "Plugin ID", example = "string")
    private String pluginId;

    @Schema(description = "Plugin type", example = "string")
    private String pluginType;
}
