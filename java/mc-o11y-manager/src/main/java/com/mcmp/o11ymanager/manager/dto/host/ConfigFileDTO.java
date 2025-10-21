package com.mcmp.o11ymanager.manager.dto.host;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for retrieving the list of config files/directories")
public class ConfigFileDTO {

    @Schema(description = "File or directory name", example = "telegraf.conf")
    private String name;

    @Schema(description = "File or directory path", example = "telegraf.conf")
    private String path;

    @Schema(description = "Indicates whether it is a directory", example = "false")
    private boolean isDirectory;

    @Schema(
            description = "List of child files/directories if it is a directory",
            example = "[\"001.conf\", \"002.conf\"]")
    private List<ConfigFileDTO> children;
}
