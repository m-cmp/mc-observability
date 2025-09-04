package com.mcmp.o11ymanager.manager.dto.host;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFileListResponseDTO {
    private String hostId;
    private String commitHash;
    private String agentType;
    private List<ConfigFileDTO> files;
}
