package com.innogrid.tabcloudit.o11ymanager.facade;

import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigFileDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigTemplateFileListResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.exception.config.FileReadingException;
import com.innogrid.tabcloudit.o11ymanager.global.definition.ConfigDefinition;
import com.innogrid.tabcloudit.o11ymanager.port.SshPort;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileFacadeService {

  private final FileService fileService;
  private final SshPort sshPort;

  @Value("${deploy.site-code}")
  private String deploySiteCode;

  @Value("${config.base-path:./config}")
  private String configBasePath;

  //agent 파일 읽기
  public String readAgentConfigFile(String uuid, Agent agent) throws FileReadingException {
    Path agentDir = resolveAgentConfigPath(uuid, agent);

    String configFileName = switch (agent) {
      case TELEGRAF -> ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG;
      case FLUENT_BIT -> ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES;
      default -> throw new FileReadingException("Unsupported agent: " + agent);
    };

    File configFile = agentDir.resolve(configFileName).toFile();

    return fileService.singleFileReader(configFile);
  }

  public String getHostConfigTelegrafRemotePath() {
    return ConfigDefinition.CMP_AGENT_ROOT_PATH + "/" +
            "sites/" +
            deploySiteCode + "/" +
            ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF + "/etc";
  }

  public String getHostConfigFluentBitRemotePath() {
    return ConfigDefinition.CMP_AGENT_ROOT_PATH + "/" +
            "sites/" +
            deploySiteCode + "/" +
            ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT + "/etc";
  }

  //agent remote path
  public String getAgentRemotePath(Agent agent) {
    return switch (agent) {
      case TELEGRAF -> getHostConfigTelegrafRemotePath();
      case FLUENT_BIT -> getHostConfigFluentBitRemotePath();
      default -> throw new FileReadingException("Unsupported agent: " + agent);
    };
  }

  //agent sub 디렉토리
  public Path resolveAgentConfigPath(String uuid, Agent agent) {
    Path hostConfigDir = Path.of(configBasePath, uuid);
    String subPath = switch (agent) {
      case TELEGRAF -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF;
      case FLUENT_BIT -> ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT;
      default -> throw new FileReadingException("Unsupported agent: " + agent);
    };
    Path agentConfigDir = hostConfigDir.resolve(subPath);
    if (!Files.exists(agentConfigDir) || !Files.isDirectory(agentConfigDir)) {
      throw new FileReadingException("Config directory not found: " + agentConfigDir);
    }
    return agentConfigDir;
  }

  public ConfigTemplateFileListResponseDTO getFluentBitTemplateFileList() {

    List<ConfigFileDTO> files = new ArrayList<>();

    ConfigFileDTO mainConfig = ConfigFileDTO.builder()
        .name(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_MAIN_CONFIG)
        .path(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_MAIN_CONFIG)
        .isDirectory(false)
        .children(new ArrayList<>())
        .build();

    ConfigFileDTO parsersConfig = ConfigFileDTO.builder()
        .name(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_PARSERS_CONFIG)
        .path(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_PARSERS_CONFIG)
        .isDirectory(false)
        .children(new ArrayList<>())
        .build();

    ConfigFileDTO logLevelLua = ConfigFileDTO.builder()
        .name(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_LOG_LEVEL_LUA)
        .path(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_LOG_LEVEL_LUA)
        .isDirectory(false)
        .children(new ArrayList<>())
        .build();

    ConfigFileDTO variables = ConfigFileDTO.builder()
        .name(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES)
        .path(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES)
        .isDirectory(false)
        .children(new ArrayList<>())
        .build();

    ConfigFileDTO addTimestamp = ConfigFileDTO.builder()
            .name(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_ADD_TIMESTAMP_LUA)
            .path(ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_ADD_TIMESTAMP_LUA)
            .isDirectory(false)
            .children(new ArrayList<>())
            .build();

    files.add(mainConfig);
    files.add(parsersConfig);
    files.add(logLevelLua);
    files.add(addTimestamp);
    files.add(variables);

    return ConfigTemplateFileListResponseDTO.builder()
        .agentType(Agent.FLUENT_BIT.getName())
        .files(files)
        .build();
  }


  public ConfigTemplateFileListResponseDTO getTelegrafTemplateFileList() {

    List<ConfigFileDTO> files = new ArrayList<>();

    ConfigFileDTO telegrafConfig = ConfigFileDTO.builder()
        .name(ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG)
        .path(ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG)
        .isDirectory(false)
        .children(new ArrayList<>())
        .build();
    files.add(telegrafConfig);

    return ConfigTemplateFileListResponseDTO.builder()
        .agentType(Agent.TELEGRAF.getName())
        .files(files)
        .build();
  }
}
