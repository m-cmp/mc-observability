package com.mcmp.o11ymanager.manager.dto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigTemplateFileListResponseDTO {
    private String agentType;
    private List<ConfigFileDTO> files;
}
