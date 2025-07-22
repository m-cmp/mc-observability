package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.host.ConfigFileContentResponseDTO;
import com.mcmp.o11ymanager.dto.host.ConfigFileListResponseDTO;
import com.mcmp.o11ymanager.dto.host.ConfigResponseDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.global.annotation.Base64Encode;
import com.mcmp.o11ymanager.global.definition.ConfigDefinition;
import com.mcmp.o11ymanager.mapper.host.ConfigMapper;
import com.mcmp.o11ymanager.model.agentHealth.SshConnection;
import com.mcmp.o11ymanager.model.config.ConfigFileNode;
import com.mcmp.o11ymanager.oldService.domain.HostDomainService;
import com.mcmp.o11ymanager.oldService.domain.interfaces.FileService;
import com.mcmp.o11ymanager.oldService.domain.interfaces.SshService;
import com.mcmp.o11ymanager.oldService.domain.interfaces.TargetService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegrafConfigFacadeService {

  private final FileService fileService;
  private final HostDomainService hostDomainService;
  private final FileFacadeService fileFacadeService;
  private final SshService sshService;
  private final ConfigMapper configMapper;

  private static final Lock configDownloadLock = new ReentrantLock();
  private final TargetService targetService;

  @Value("${deploy.site-code}")
  private String deploySiteCode;

  @Value("${influxdb.url}")
  private String influxDBURL;

  @Value("${influxdb.database}")
  private String influxDBDatabase;

  @Value("${influxdb.username}")
  private String influxDBUsername;

  @Value("${influxdb.password}")
  private String influxDBPassword;

  @Value("${config.base-path:./config}")
  private String configBasePath;

  private final ClassPathResource telegrafConfigTemplate = new ClassPathResource(
      "telegraf_template.conf");

  private final ClassPathResource telegrafConfigGlobal = new ClassPathResource("telegraf_global");
  private final ClassPathResource telegrafConfigAgent = new ClassPathResource("telegraf_agent");
  private final ClassPathResource telegrafConfigInputsCPU = new ClassPathResource(
      "telegraf_inputs_cpu");
  private final ClassPathResource telegrafConfigInputsDisk = new ClassPathResource(
      "telegraf_inputs_disk");
  private final ClassPathResource telegrafConfigInputsDiskIO = new ClassPathResource(
      "telegraf_inputs_diskio");
  private final ClassPathResource telegrafConfigInputsMem = new ClassPathResource(
      "telegraf_inputs_mem");
  private final ClassPathResource telegrafConfigInputsNet = new ClassPathResource(
      "telegraf_inputs_net");
  private final ClassPathResource telegrafConfigInputsProcesses = new ClassPathResource(
      "telegraf_inputs_processes");
  private final ClassPathResource telegrafConfigInputsProcstat = new ClassPathResource(
      "telegraf_inputs_procstat");
  private final ClassPathResource telegrafConfigInputsSwap = new ClassPathResource(
      "telegraf_inputs_swap");
  private final ClassPathResource telegrafConfigInputsSystem = new ClassPathResource(
      "telegraf_inputs_system");
  private final ClassPathResource telegrafConfigInputsNVIDIASMI = new ClassPathResource(
      "telegraf_inputs_nvidia_smi");
  private final ClassPathResource telegrafConfigOutputsInfluxDB = new ClassPathResource(
      "telegraf_outputs_influxdb");

  public static final String CONFIG_METRIC_CPU = "cpu";
  public static final String CONFIG_METRIC_DISK = "disk";
  public static final String CONFIG_METRIC_DISKIO = "diskio";
  public static final String CONFIG_METRIC_MEM = "mem";
  public static final String CONFIG_METRIC_NET = "net";
  public static final String CONFIG_METRIC_PROCESSES = "processes";
  public static final String CONFIG_METRIC_PROCSTAT = "procstat";
  public static final String CONFIG_METRIC_SWAP = "swap";
  public static final String CONFIG_METRIC_SYSTEM = "system";
  public static final String CONFIG_METRIC_GPU = "gpu";

  public static final String CONFIG_DEFAULT_METRICS =
      CONFIG_METRIC_CPU + "," +
          CONFIG_METRIC_DISK + "," +
          CONFIG_METRIC_DISKIO + "," +
          CONFIG_METRIC_MEM + "," +
          CONFIG_METRIC_NET + "," +
          CONFIG_METRIC_PROCESSES + "," +
          CONFIG_METRIC_PROCSTAT + "," +
          CONFIG_METRIC_SWAP + "," +
          CONFIG_METRIC_SYSTEM;

  public void initConfig(String targetId, Path telegrafBaseDir, String cloudService) {

    // 1) telegraf.conf 만들 위치 지정
    Path telegraConfFilePath = telegrafBaseDir.resolve(
            ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG);

    // 2) telegraf.conf 파일 작성
    String configContent = generateTelegrafConfig(targetId, CONFIG_DEFAULT_METRICS, cloudService);

    // 3) 파일 생성
    File telegrafConfigFile = new File(String.valueOf(telegraConfFilePath));

    log.info("Updating telegraf config file: " + telegrafConfigFile.getAbsolutePath());

    // 4) 파일 저장
    fileService.generateFile(telegrafConfigFile, configContent);
  }

  public void updateTelegrafConfig(String targetId, String content, String path) {

    // 1) TelegrafConf 위치 확인
    Path telegrafConfPath = fileFacadeService.resolveAgentConfigPath(targetId, Agent.TELEGRAF);

    // 2) 파일 작성
    File updatedConfigFile = new File(String.valueOf(telegrafConfPath));
    fileService.writeFile(updatedConfigFile, path, content);
  }

  public Path getTelegrafConfigWorkingPath(String targetId) {
    return fileFacadeService.resolveAgentConfigPath(targetId, Agent.TELEGRAF);
  }


  // 원격에 있는 파일 내용 가져와서 로컬에 복사
  public void downloadTelegrafConfig(TargetRegisterDTO target) throws IOException {
    try {
      configDownloadLock.lock();

      // 1) SSH로 원격 파일 확인
      log.debug(target.getAccessInfo().getIp(), target.getAccessInfo().getPort(), target.getAccessInfo().getUser());

      SshConnection connection = sshService.getConnection(
              target.getAccessInfo().getIp(),
              target.getAccessInfo().getPort(),
              target.getAccessInfo().getUser(),
              target.getAccessInfo().getSshKey()
      );

      // 2) 원격에 파일 없을시 종료
      if (!sshService.isExistTelegrafConfigDirectory(connection)) {
        return;
      }

      // 원격에 파일 있을 경우 아래 내용 실행
      // 3) 로컬에 telegraf 폴더 생성
      Path path = Path.of(configBasePath, target.getName(),
              ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF);
      fileService.createDirectory(path);

      // 4) 원격 파일 내용 가져오기
      sshService.download(connection, fileFacadeService.getHostConfigTelegrafRemotePath(),
              path, target.getAccessInfo().getUser(), target.getAccessInfo().getIp(), target.getAccessInfo().getPort(), target.getAccessInfo().getSshKey());
    } finally {
      configDownloadLock.unlock();
    }
  }

  public void initTelegrafConfig(Optional<TargetEntity> target,
      String cloudService) {
    // 1) 로컬에 telegraf 폴더 생성
    Path path = Path.of(configBasePath, target.get().getId(),
        ConfigDefinition.HOST_CONFIG_SUB_FOLDER_NAME_TELEGRAF);

    Path configDir = fileService.createDirectory(path);

    log.info("로컬 경로 : {}", configDir.toString());

    // 2) 로컬 파일 복사하여 telegraf.conf 생성
    initConfig(target.get().getId(), path, cloudService);
  }

  @Base64Encode
  public ConfigFileContentResponseDTO getTelegrafConfigContent(String requestId, String targetId, String path) {


    targetService.findById(targetId);

    // TODO: Get content from config folder directly
//    if (commitHash == null || commitHash.isEmpty()) {
//      commitHash = host.getMonitoring_agent_config_git_hash();
//    }
//
//    String content = gitFacadeService.getFileContentOfCommitHash(requestId, host.getId(),
//        Agent.TELEGRAF, commitHash, path);

    return ConfigFileContentResponseDTO.builder()
        .hostId(targetId)
//        .commitHash(commitHash)
        .path(path)
//        .content(content)
        .content("TODO")
        .build();
  }


  public ConfigFileListResponseDTO getTelegrafConfigFileList(String targetId) {

    Optional<TargetEntity> target = targetService.findById(targetId);


    // TODO: Get file list from config folder directly
    List<ConfigFileNode> configFiles = null;
//    List<ConfigFileNode> configFiles = gitFacadeService.getConfigFileList(requestId, host.getId(),
//            commitHash, Agent.TELEGRAF);

    return ConfigFileListResponseDTO.builder()
        .hostId(targetId)
        .agentType(Agent.TELEGRAF.getName())
        .files(configMapper.toFileDTOList(configFiles))
        .build();

  }


  @Base64Encode
  public ConfigResponseDTO getTelegrafConfigTemplate(String path) {
    if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG)) {
      return ConfigResponseDTO.builder()
          .content(fileService.getClassResourceContent(telegrafConfigTemplate)).build();
    }

    throw new IllegalArgumentException("Invalid Telegraf template path");
  }


  public String generateTelegrafConfig(String targetId, String metrics,String cloudService) {
    String errMsg;

    if (!telegrafConfigGlobal.exists()) {
      errMsg = "Invalid filePath : telegrafConfigGlobal";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigAgent.exists()) {
      errMsg = "Invalid filePath : telegrafConfigAgent";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsCPU.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsCPU";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsDisk.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsDisk";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsDiskIO.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsDiskIO";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsMem.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsMem";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsNet.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsNet";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsProcesses.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsProcesses";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsProcstat.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsProcstat";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsSwap.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsSwap";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsSystem.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsSystem";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigInputsNVIDIASMI.exists()) {
      errMsg = "Invalid filePath : telegrafConfigInputsNVIDIASMI";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    if (!telegrafConfigOutputsInfluxDB.exists()) {
      errMsg = "Invalid filePath : telegrafConfigOutputsInfluxDB";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    }

    StringBuilder sb = new StringBuilder();

    fileService.appendConfig(telegrafConfigGlobal, sb);
    fileService.appendConfig(telegrafConfigAgent, sb);

    String[] metricsSplit = Arrays.stream(metrics.replace(" ", "").split(","))
        .distinct()
        .toArray(String[]::new);

    for (String metric : metricsSplit) {
      switch (metric) {
        case CONFIG_METRIC_CPU:
          fileService.appendConfig(telegrafConfigInputsCPU, sb);
          break;
        case CONFIG_METRIC_DISK:
          fileService.appendConfig(telegrafConfigInputsDisk, sb);
          break;
        case CONFIG_METRIC_DISKIO:
          fileService.appendConfig(telegrafConfigInputsDiskIO, sb);
          break;
        case CONFIG_METRIC_MEM:
          fileService.appendConfig(telegrafConfigInputsMem, sb);
          break;
        case CONFIG_METRIC_NET:
          fileService.appendConfig(telegrafConfigInputsNet, sb);
          break;
        case CONFIG_METRIC_PROCESSES:
          fileService.appendConfig(telegrafConfigInputsProcesses, sb);
          break;
        case CONFIG_METRIC_PROCSTAT:
          fileService.appendConfig(telegrafConfigInputsProcstat, sb);
          break;
        case CONFIG_METRIC_SWAP:
          fileService.appendConfig(telegrafConfigInputsSwap, sb);
          break;
        case CONFIG_METRIC_SYSTEM:
          fileService.appendConfig(telegrafConfigInputsSystem, sb);
          break;
        case CONFIG_METRIC_GPU:
          fileService.appendConfig(telegrafConfigInputsNVIDIASMI, sb);
          break;
        default:
          throw new RuntimeException("Invalid metric: " + metric);
      }
    }

    fileService.appendConfig(telegrafConfigOutputsInfluxDB, sb);

    String finalTargetId = (targetId != null) ? targetId : "";
    log.debug(finalTargetId);

    String finalCloudService = (cloudService != null) ? cloudService : "";
    log.debug(finalCloudService);

    String finalInfluxDBURL = (influxDBURL != null) ? influxDBURL : "";
    String finalInfluxDBDatabase = (influxDBDatabase != null) ? influxDBDatabase : "";
    String finalInfluxDBUsername = (influxDBUsername != null) ? influxDBUsername : "";
    String finalInfluxDBPassword = (influxDBPassword != null) ? influxDBPassword : "";

    return sb.toString()
        .replace("@SITE_CODE", deploySiteCode)
        .replace("@ID", finalTargetId)
        .replace("@CLOUD_SERVICE", finalCloudService)
        .replace("@URL", finalInfluxDBURL)
        .replace("@DATABASE", finalInfluxDBDatabase)
        .replace("@USERNAME", finalInfluxDBUsername)
        .replace("@PASSWORD", finalInfluxDBPassword);
  }


}
