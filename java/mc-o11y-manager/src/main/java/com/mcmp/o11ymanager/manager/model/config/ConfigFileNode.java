package com.mcmp.o11ymanager.manager.model.config;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFileNode {
    private String name;
    private String path;
    private boolean isDirectory;
    private List<ConfigFileNode> children;
}
