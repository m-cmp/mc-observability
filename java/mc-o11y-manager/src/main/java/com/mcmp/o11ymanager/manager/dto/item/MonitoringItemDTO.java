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
public class MonitoringItemDTO {
    @Schema(description = "Monitoring item sequence", example = "0")
    private Long seq;

    @Schema(description = "Namespace ID", example = "ns-1")
    private String nsId;

    @Schema(description = "Infra ID", example = "infra-1")
    private String infraId;

    @Schema(description = "Node ID", example = "node-1")
    private String nodeId;

    @Schema(description = "VM name", example = "mcmp-vm")
    private String name;

    @Schema(description = "Monitoring item state", example = "string")
    private String state;

    @Schema(description = "Plugin sequence", example = "1")
    private Long pluginSeq;

    @Schema(description = "Plugin name", example = "cpu")
    private String pluginName;

    @Schema(description = "Plugin type", example = "INPUT")
    private String pluginType;

    @Schema(description = "Plugin configuration", example = "string")
    private String pluginConfig;
}
