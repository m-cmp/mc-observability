package com.mcmp.o11ymanager.manager.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringItemRequestDTO {
    @Schema(description = "Plugin sequence", example = "0")
    private Long pluginSeq;

    @Schema(description = "Plugin configuration", example = "string")
    private String pluginConfig;
}
