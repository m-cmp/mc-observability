package com.mcmp.o11ymanager.manager.mapper.host;

import com.mcmp.o11ymanager.manager.dto.host.ConfigFileDTO;
import com.mcmp.o11ymanager.manager.model.config.ConfigFileNode;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapper {

    public ConfigFileDTO toFileDTO(ConfigFileNode node) {
        if (node == null) {
            return null;
        }

        List<ConfigFileDTO> children = null;
        if (node.getChildren() != null) {
            children =
                    node.getChildren().stream().map(this::toFileDTO).collect(Collectors.toList());
        }

        return ConfigFileDTO.builder()
                .name(node.getName())
                .path(node.getPath())
                .isDirectory(node.isDirectory())
                .children(children)
                .build();
    }

    public List<ConfigFileDTO> toFileDTOList(List<ConfigFileNode> nodes) {
        if (nodes == null) {
            return null;
        }

        return nodes.stream().map(this::toFileDTO).collect(Collectors.toList());
    }
}
