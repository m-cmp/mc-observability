package com.innogrid.tabcloudit.o11ymanager.facade;

import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigFileContentResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigFileListResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostConnectionDTO;
import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.global.annotation.Base64Encode;
import com.innogrid.tabcloudit.o11ymanager.global.definition.ConfigDefinition;
import com.innogrid.tabcloudit.o11ymanager.mapper.host.ConfigMapper;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.SshConnection;
import com.innogrid.tabcloudit.o11ymanager.model.config.ConfigFileNode;
import com.innogrid.tabcloudit.o11ymanager.service.domain.HostDomainService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.*;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class FluentBitConfigFacadeService {

  private final FileService fileService;
  private final GitFacadeService gitFacadeService;
  private final HostDomainService hostDomainService;
  private final FileFacadeService fileFacadeService;
  private final SshService sshService;
  private final GitService gitService;
  private final HostService hostService;
  private final ConfigMapper configMapper;

  @Value("${loki.url}")
  private String lokiURL;

  @Value("${config.base-path:./config}")
  private String configBasePath;


  private final ClassPathResource fluentBitMainConfig = new ClassPathResource("fluent-bit.conf");
  private final ClassPathResource fluentBitVariables = new ClassPathResource(
      "fluent-bit_variables");
  private final ClassPathResource fluentBitLogLevelLua = new ClassPathResource("log-level.lua");
  private final ClassPathResource fluentBitAddTimestampLua = new ClassPathResource(
      "add-timestamp.lua");
  private final ClassPathResource fluentBitParsersConf = new ClassPathResource("parsers.conf");


  public void initConfig(String id, String credentialId, String cloudService, String hostType,
      Path fluentbitBaseDir) {

    // 1) 동기 작업 방지
    ReentrantLock lock = gitFacadeService.getRepositoryLock(id, Agent.FLUENT_BIT);

    try {
      lock.lock();

      // 2 파일 생성 및 저장
      // 2-1) variables 파일 생성
      String configContent = generateFluentBitVariablesFile(id, credentialId, cloudService,
          hostType);
      Path variabelsFilePath = fluentbitBaseDir.resolve(
          ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES);
      File fluentbitVariables = new File(String.valueOf(variabelsFilePath));
      log.info("Updating variables file: " + fluentbitVariables.getAbsolutePath());
      fileService.generateFile(fluentbitVariables, configContent);

      // 2-2) fluent-bit.conf 파일 생성
      String fluentBitConfContent = fileService.getFileContent(fluentBitMainConfig);
      Path fluentbitConfFilePath = fluentbitBaseDir.resolve(
          ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_MAIN_CONFIG);
      File fluentBitConf = new File(String.valueOf(fluentbitConfFilePath));
      log.info("Updating fluent-bit.conf file: " + fluentBitConf.getAbsolutePath());
      fileService.generateFile(fluentBitConf, fluentBitConfContent);

      // 2-3) parsers.conf 파일 생성
      String parsersConfContent = fileService.getFileContent(fluentBitParsersConf);
      Path parsersFilePath = fluentbitBaseDir.resolve(
          ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_PARSERS_CONFIG);
      File parsersConf = new File(String.valueOf(parsersFilePath));
      log.info("Updating parsers file: " + parsersConf.getAbsolutePath());
      fileService.generateFile(parsersConf, parsersConfContent);

      // 2-4) log-level.lua 파일 생성
      String logLevelLuaContent = fileService.getFileContent(fluentBitLogLevelLua);
      Path logLevelLuaFilePath = fluentbitBaseDir.resolve(
          ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_LOG_LEVEL_LUA);
      File logLevelLua = new File(String.valueOf(logLevelLuaFilePath));
      log.info("Updating logLevelLua file: " + logLevelLua.getAbsolutePath());
      fileService.generateFile(logLevelLua, logLevelLuaContent);


    } finally {
      lock.unlock();
    }
  }

  // 원격에 있는 파일 내용 가져와서 로컬에 복사
  public void downloadFluentbitConfig(HostConnectionDTO host) throws IOException {
    ReentrantLock lock = gitFacadeService.getRepositoryLock(host.getHostId(), Agent.TELEGRAF);

    try {
      lock.lock();

      // 1) SSH로 원격 파일 확인
      log.debug(host.getIp(), host.getPort(), host.getUserId(), host.getPassword());

      SshConnection connection = sshService.getConnection(
              host.getIp(),
              host.getPort(),
              host.getUserId(),
              host.getPassword()
      );

      // 2) 원격에 파일 없을시 종료
      if (!sshService.isExistFluentbitConfigDirectory(connection)) {
        return;
      }

      // 원격에 파일 있을 경우 아래 내용 실행
      // 3) 로컬에 fluentbit 폴더 생성
      Path path = Path.of(configBasePath, host.getHostId(),
              ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT);

      fileService.deleteDirectoryExceptGitByHostId(host.getHostId());
      Path configDir = fileService.createDirectory(path);

      // 4) git 초기화
      gitService.init(configDir.toFile());

      // 5) 원격 파일 내용 가져오기
      sshService.download(connection, fileFacadeService.getHostConfigFluentBitRemotePath(),
              path, host.getUserId(), host.getIp(), host.getPort(), host.getPassword());

      String commitMessage = "Config updated (Fluentbit)";

      // 6) Git 커밋
      Git git = gitService.getGit(path.toFile());
      gitService.commit(git, ".", commitMessage, "innogrid", "cmp@innogrid.com");

      // 7) Git 커밋 해시 업데이트
      String commitHash = gitService.getHashName(git);
      hostService.updateMonitoringAgentConfigGitHash(host.getHostId(), commitHash);
    } finally {
      lock.unlock();
    }
  }

  public void initFluentbitConfig(HostConnectionDTO host, String type, String credentialId,
      String cloudService) {
    log.debug(host.getIp(), host.getPort(), host.getUserId(), host.getPassword());

    // 1) 로컬에 fluentbit 폴더 생성
    Path path = Path.of(configBasePath, host.getHostId(),
        ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_FLUENTBIT);

    Path configDir = fileService.createDirectory(path);

    log.info("로컬 경로 : {}", configDir.toString());

    // 2) git 초기화
    gitService.init(configDir.toFile());

    // 3) 로컬에 파일 생성
    initConfig(host.getHostId(), credentialId, cloudService, type, path);

    // 4) Git 커밋
    Git git = gitService.getGit(path.toFile());
    String commitMessage = "Config initialized";
    gitService.commit(git, ".", commitMessage, "innogrid", "cmp@innogrid.com");

    // 5) Git 커밋 해시 업데이트
    String commitHash = gitService.getHashName(git);
    log.debug(">>>> DEBUG: About to log commit hash. Git object: {}", git);
    log.debug("log agent git hash : {}", commitHash);
    hostService.updateLogAgentConfigGitHash(host.getHostId(), commitHash);
  }

  public void updateFluentBitConfig(String hostId, String content) {

    // 1) 동기 작업 방지
    ReentrantLock lock = gitFacadeService.getRepositoryLock(hostId, Agent.FLUENT_BIT);

    try {
      lock.lock();

      // 1) FluentbitConf 위치 확인
      Path fluentbitConfPath = fileFacadeService.resolveAgentConfigPath(hostId, Agent.FLUENT_BIT);

      // 2) 파일 작성
      File updatedConfigFile = new File(String.valueOf(fluentbitConfPath));
      fileService.writeFile(updatedConfigFile, updatedConfigFile.getName(), content);

    } finally {
      lock.unlock();
    }

  }

  public Path getFluentbitConfigWorkingPath(String hostId) {
    return fileFacadeService.resolveAgentConfigPath(hostId, Agent.FLUENT_BIT);
  }


  public ConfigFileListResponseDTO getFluentBitConfigFileList(String requestId, String hostId,
      String commitHash) {

    HostEntity host = hostDomainService.getHostById(requestId, hostId);

    if (commitHash == null || commitHash.isEmpty()) {
      commitHash = host.getLog_agent_config_git_hash();
    }

    List<ConfigFileNode> configFiles = gitFacadeService.getConfigFileList(requestId, host.getId(),
        commitHash, Agent.FLUENT_BIT);

    return ConfigFileListResponseDTO.builder()
        .hostId(host.getId())
        .commitHash(commitHash)
        .agentType(Agent.FLUENT_BIT.getName())
        .files(configMapper.toFileDTOList(configFiles))
        .build();

  }


  @Base64Encode
  public ConfigFileContentResponseDTO getFluentBitConfigContent(String requestId, String hostId,
      String commitHash, String path) {

    HostEntity host = hostDomainService.getHostById(requestId, hostId);

    if (commitHash == null || commitHash.isEmpty()) {
      commitHash = host.getLog_agent_config_git_hash();
    }

    String content = gitFacadeService.getFileContentOfCommitHash(requestId, host.getId(),
        Agent.FLUENT_BIT, commitHash, path);

    ConfigFileContentResponseDTO response = ConfigFileContentResponseDTO.builder()
        .hostId(host.getId())
        .commitHash(commitHash)
        .path(path)
        .content(content)
        .build();

    log.info("getFluentBitConfigContent response: " + response.toString());

    return response;

  }


  //variables 파일 생성
  public String generateFluentBitVariablesFile(String uuid, String credentialId,
      String cloudService, String hostType) {
    {
      String template = fileService.getFileContent(fluentBitVariables);

      String[] lokiURLSplit = lokiURL.split("://");
      if (lokiURLSplit.length != 2) {
        throw new RuntimeException("Manager's Loki URL is invalid!");
      }
      lokiURLSplit = lokiURLSplit[1].split(":");
      if (lokiURLSplit.length < 1) {
        throw new RuntimeException("Manager's Loki URL is invalid!");
      }

      String lokiHost = lokiURLSplit[0];

      StringBuilder sb = new StringBuilder();

      fileService.appendConfig(fluentBitVariables, sb);

      uuid = uuid != null ? uuid : "";
      lokiHost = lokiHost != null ? lokiHost : "";
      credentialId = credentialId != null ? credentialId : "";
      cloudService = cloudService != null ? cloudService : "";
      hostType = hostType != null ? hostType : "";

      log.debug("uuid={}, credentialId={}, cloudService={}, hostType={}", uuid, credentialId,
          cloudService, hostType);

      return template
          .replace("@ID", uuid)
          .replace("@LOKI_HOST", lokiHost)
          .replace("@CREDENTIAL_ID", credentialId)
          .replace("@CLOUD_SERVICE", cloudService)
          .replace("@TYPE", hostType);

    }
  }


  //fluentbit 파일 템플릿
  @Base64Encode
  public ConfigResponseDTO getFluentBitConfigTemplate(String path) {
    if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_MAIN_CONFIG)) {
      return ConfigResponseDTO.builder()
          .content(fileService.getClassResourceContent(fluentBitMainConfig)).build();
    } else if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_PARSERS_CONFIG)) {
      return ConfigResponseDTO.builder()
          .content(fileService.getClassResourceContent(fluentBitParsersConf)).build();
    } else if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_LOG_LEVEL_LUA)) {
      return ConfigResponseDTO.builder()
          .content(fileService.getClassResourceContent(fluentBitLogLevelLua)).build();
    } else if (Objects.equals(path,
        ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_ADD_TIMESTAMP_LUA)) {
      return ConfigResponseDTO.builder()
          .content(fileService.getClassResourceContent(fluentBitAddTimestampLua)).build();
    } else if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES)) {
      return ConfigResponseDTO.builder()
          .content(fileService.getClassResourceContent(fluentBitVariables)).build();
    }

    throw new IllegalArgumentException("Invalid Fluent-Bit template path");
  }


  //FluentBit 리소스 파일
  public ClassPathResource getFluentBitResource(String path) {
    return switch (path) {
      case ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_MAIN_CONFIG -> fluentBitMainConfig;
      case ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_PARSERS_CONFIG -> fluentBitParsersConf;
      case ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_LOG_LEVEL_LUA -> fluentBitLogLevelLua;
      case ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES -> fluentBitVariables;
      default -> throw new IllegalArgumentException("Invalid Fluent-Bit resource path");
    };
  }


}
