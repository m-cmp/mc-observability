package com.mcmp.o11ymanager.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
