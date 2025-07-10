package com.innogrid.tabcloudit.o11ymanager.mapper.host;

import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigFileDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigHistoryDTO;
import com.innogrid.tabcloudit.o11ymanager.global.definition.TimestampDefinition;
import com.innogrid.tabcloudit.o11ymanager.model.config.ConfigFileNode;
import com.innogrid.tabcloudit.o11ymanager.model.config.GitCommit;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConfigMapper {

  public ConfigHistoryDTO toHistoryDTO(GitCommit commit) {
    return ConfigHistoryDTO.builder()
        .commitHash(commit.getCommitHash())
        .message(commit.getMessage())
        .timestamp(commit.getTimestamp().toLocalDateTime().format(DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)))
        .build();
  }

  public ConfigFileDTO toFileDTO(ConfigFileNode node) {
    if (node == null) {
      return null;
    }

    List<ConfigFileDTO> children = null;
    if (node.getChildren() != null) {
      children = node.getChildren().stream()
              .map(this::toFileDTO)
              .collect(Collectors.toList());
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

    return nodes.stream()
            .map(this::toFileDTO)
            .collect(Collectors.toList());
  }
}
