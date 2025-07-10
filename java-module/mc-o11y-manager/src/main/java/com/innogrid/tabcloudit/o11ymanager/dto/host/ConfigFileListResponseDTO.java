package com.innogrid.tabcloudit.o11ymanager.dto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
