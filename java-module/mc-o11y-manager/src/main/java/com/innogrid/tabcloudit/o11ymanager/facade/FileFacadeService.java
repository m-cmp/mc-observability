package com.innogrid.tabcloudit.o11ymanager.facade;

import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigFileDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigTemplateFileListResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostConnectionDTO;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.exception.config.FileReadingException;
import com.innogrid.tabcloudit.o11ymanager.exception.git.GitCommitContentsException;
import com.innogrid.tabcloudit.o11ymanager.global.definition.ConfigDefinition;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.AgentCommandResult;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.SshConnection;
import com.innogrid.tabcloudit.o11ymanager.port.SshPort;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
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


  //agent config 보내기
  public void sendConfigs(String requestId, HostConnectionDTO host, Agent agent) {

    Path agentConfigPath = resolveAgentConfigPath(host.getHostId(), agent);
    File agentConfigDir = agentConfigPath.toFile();

    String remotePath = getAgentRemotePath(agent);

    List<File> filesToSend = fileService.getFilesRecursively(agentConfigDir);
    if (filesToSend.isEmpty()) {
      log.warn("[{}] No files to send for {} at {}", requestId, agent, agentConfigDir);
      return;
    }

    SshConnection sshConnection = null;
    try {
      String username = host.getUserId();
      String ip = host.getIp();
      int port = host.getPort();
      String password = host.getPassword();

      sshConnection = sshPort.openSession(username, ip, port, password);

      String mkdirCmd = "mkdir -p " + remotePath;
      AgentCommandResult mkdirResult = sshPort.executeCommand(sshConnection, mkdirCmd);
      if (mkdirResult.getExitCode() != 0) {
        String errMsg = "Failed to create remote directory: " + remotePath + ", Error: "
                + mkdirResult.getError();
        log.error(errMsg);
        throw new IOException(errMsg);
      }

      for (File file : filesToSend) {
        String relativeFilePath = agentConfigDir.toPath().relativize(file.toPath()).toString();
        String remoteFilePath = remotePath + "/" + relativeFilePath;

        String remoteParentDir = new File(remoteFilePath).getParent();
        if (remoteParentDir != null) {
          String mkdirParentCmd = "mkdir -p " + remoteParentDir;
          AgentCommandResult mkdirParentResult = sshPort.executeCommand(sshConnection,
                  mkdirParentCmd);
          if (mkdirParentResult.getExitCode() != 0) {
            log.warn("Failed to create remote parent directory: {}, Error: {}",
                    remoteParentDir, mkdirParentResult.getError());
          }
        }

        String fileContent = fileService.singleFileReader(file);

        fileContent = fileContent.replace("'", "'\\''");

        String writeCmd = "cat > '" + remoteFilePath + "' << 'EOL'\n" + fileContent + "\nEOL";
        AgentCommandResult writeResult = sshPort.executeCommand(sshConnection, writeCmd);

        if (writeResult.getExitCode() != 0) {
          log.error("Failed to write file to remote path: {}, Error: {}",
                  remoteFilePath, writeResult.getError());
        } else {
          log.info("Successfully transferred file to: {}", remoteFilePath);
        }
      }

      log.info("Successfully sent all configuration files from {} to remote path: {}",
              agentConfigDir, remotePath);

    } catch (Exception e) {
      String errMsg = "Failed to send config files to remote host: " + e.getMessage();
      log.error(errMsg, e);
      throw new GitCommitContentsException();
    } finally {
      if (sshConnection != null) {
        try {
          sshConnection.getSession().close();
          sshConnection.getClient().stop();
        } catch (Exception e) {
          log.warn("Error closing SSH connection: {}", e.getMessage());
        }
      }
    }
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
