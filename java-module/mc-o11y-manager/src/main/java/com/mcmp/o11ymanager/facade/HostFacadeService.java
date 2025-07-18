package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.host.*;
import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.enums.AgentAction;
import com.mcmp.o11ymanager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.enums.ResponseStatus;
import com.mcmp.o11ymanager.event.AgentHistoryEvent;
import com.mcmp.o11ymanager.event.AgentHistoryFailEvent;
import com.mcmp.o11ymanager.exception.host.*;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.global.definition.TimestampDefinition;
import com.mcmp.o11ymanager.infrastructure.util.CheckUtil;
import com.mcmp.o11ymanager.mapper.host.ConfigMapper;
import com.mcmp.o11ymanager.mapper.host.HostMapper;
import com.mcmp.o11ymanager.model.host.HostStatus;
import com.mcmp.o11ymanager.repository.HostJpaRepository;
import com.mcmp.o11ymanager.service.AgentHealthCheckServiceImpl;
import com.mcmp.o11ymanager.service.domain.HostDomainService;
import com.mcmp.o11ymanager.service.interfaces.FileService;
import com.mcmp.o11ymanager.service.interfaces.HostService;
import com.mcmp.o11ymanager.service.interfaces.SshService;
import com.mcmp.o11ymanager.service.interfaces.TcpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostFacadeService {

  private final HostService hostService;
  private final AgentHealthCheckServiceImpl agentHealthCheckService;
  private final SshService sshService;
  private final RequestInfo requestInfo;
  private final HostMapper hostMapper;
  private final HostDomainService hostDomainService;

  private final ApplicationEventPublisher event;
  private final TcpService tcpService;

  private final FileService fileService;

  private final HostJpaRepository hostJpaRepository;

  private final Map<String, HostResponseDTO> processingHost = new ConcurrentHashMap<>();
  private final TelegrafConfigFacadeService telegrafConfigFacadeService;
  private final FluentBitConfigFacadeService fluentBitConfigFacadeService;
  private ExecutorService checkAgentsExecutor;
  private static final int AGENT_CHECK_THREAD_MAX = 30;
  private final ConfigMapper configMapper;

  private final ConcurrentHashMap<String, ReentrantLock> repositoryLocks = new ConcurrentHashMap<>();

  private ReentrantLock getHostLock(String uuid) {
    return repositoryLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
  }

  public List<HostResponseDTO> list() {
    List<HostDTO> hosts = hostService.list();

    List<HostResponseDTO> results = new ArrayList<>();
    for (HostDTO host : hosts) {
      HostResponseDTO dto = HostResponseDTO.builder()
          .id(host.getId())
          .name(host.getName())
          .hostname(host.getHostname())
          .ip(host.getIp())
          .port(host.getPort())
          .hostStatus(host.getHost_status())
          .monitoringServiceStatus(host.getMonitoringServiceStatus())
          .logServiceStatus(host.getLogServiceStatus())
          .monitoringAgentConfigGitHash(host.getMonitoring_agent_config_git_hash())
          .logAgentConfigGitHash(host.getLog_agent_config_git_hash())
          .monitoringAgentVersion(host.getMonitoring_agent_version())
          .logAgentVersion(host.getLog_agent_version())
          .description(host.getDescription())
          .type(host.getType())
          .user(host.getUser())
          .cloudService(host.getCloudService())
          .credentialId(host.getCredentialId())
          .createdAt(host.getCreatedAt() != null ? host.getCreatedAt()
              .format(DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)) : null)
          .updatedAt(host.getUpdatedAt() != null ? host.getUpdatedAt()
              .format(DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)) : null)
          .build();
      results.add(dto);
    }
    return results;
  }

  public HostResponseDTO getHost(String id) {
    HostDTO host = hostService.findById(id);

    return hostMapper.toDTO(host.toEntity());
  }

  @Transactional
  public List<ResultDTO> create(List<HostCreateDTO> hosts) {

    List<ResultDTO> results = new ArrayList<>();

    for (HostCreateDTO host : CheckUtil.emptyIfNull(hosts)) {
      ReentrantLock hostLock = getHostLock(host.getId());

      try {
        hostLock.lock();

        // 1) Ping 체크
        String ip = host.getIps().stream()
            .filter(i -> tcpService.checkServerStatus(i, host.getPort()).equals(HostStatus.RUNNING))
            .findFirst()
            .orElseThrow(
                () -> new HostConnectionFailedException(requestInfo.getRequestId(), host.getId()));
        int port = host.getPort();

        // 2) 중복 검사
        hostService.validateUniqueIpPort(ip, port);

        // 3) SSH 연결 확인
        sshService.getConnection(ip, host.getPort(), host.getUser(),
            host.getPassword());

        // 4) Host DB 저장
        HostDTO saved = hostService.create(host, ip);
        HostConnectionDTO connectionDTO = hostService.getHostConnectionInfo(saved.getId());

        // 5) Telegraf 설정 복사
        telegrafConfigFacadeService.downloadTelegrafConfig(connectionDTO);

        // 6) Fluentbit 설정 복사
        fluentBitConfigFacadeService.downloadFluentbitConfig(connectionDTO);

        // 7) 이력 이벤트 발행
        AgentHistoryEvent successEvent = new AgentHistoryEvent(
            requestInfo.getRequestId(),
            AgentAction.HOST_CREATED,
            host.getId(),
            requestInfo.getRequestUserId(),
            ""
        );
        event.publishEvent(successEvent);

        // 8) 응답값 생성
        results.add(ResultDTO.builder()
            .id(saved.getId())
            .status(ResponseStatus.SUCCESS)
            .build());

      } catch (Exception e) {
        event.publishEvent(new AgentHistoryFailEvent(
            requestInfo.getRequestId(),
            AgentAction.HOST_CREATED,
            host.getId(),
            requestInfo.getRequestUserId(),
            e.getMessage()
        ));

        results.add(ResultDTO.builder()
            .id(host.getId())
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      } finally {
        hostLock.unlock();
      }
    }

    return results;
  }


  @Transactional
  public List<ResultDTO> update(HostUpdateDTO request) {

    List<ResultDTO> result = new ArrayList<>();

    for (String hostId : CheckUtil.emptyIfNull(request.getHost_id_list())) {
      ReentrantLock hostLock = getHostLock(hostId);

      try {
        hostLock.lock();

        // 1) Host 조회
        HostDTO host = hostService.findById(hostId);

        // 2) SSH 갱신 후 연결 확인(새로운 계정)
        sshService.updateConnection(host.getIp(), request.getData().getPort(),
            request.getData().getUser(), request.getData().getPassword());

        // 3) DB 수정
        hostService.update(hostId, request);

        // 4) 성공 이벤트 발행
        AgentHistoryEvent successEvent = new AgentHistoryEvent(
            requestInfo.getRequestId(),
            AgentAction.HOST_UPDATED,
            hostId,
            requestInfo.getRequestUserId(),
            ""
        );
        event.publishEvent(successEvent);

        // 5) 리턴할 값 저장
        result.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.SUCCESS)
            .build());

      } catch (Exception e) {
        AgentHistoryFailEvent failureEvent = new AgentHistoryFailEvent(
            requestInfo.getRequestId(),
            AgentAction.HOST_UPDATED,
            hostId,
            requestInfo.getRequestUserId(),
            e.getMessage()
        );
        event.publishEvent(failureEvent);

        result.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      } finally {
        hostLock.unlock();
      }
    }
    return result;
  }

  private void checkAgents() {
    try {
      List<HostResponseDTO> hostResponseDTOList = hostService.list().stream()
          .map(i -> hostMapper.toDTO(i.toEntity())).toList();
      log.debug("HostService - Checking agents for {} hosts...", hostResponseDTOList.size());

      if (hostResponseDTOList.isEmpty()) {
        log.debug("HostService - Host list is empty. No need to check agents for hosts.");
        return;
      }

      checkAgentsExecutor = Executors.newFixedThreadPool(AGENT_CHECK_THREAD_MAX);
      for (final HostResponseDTO hostResponseDTO : hostResponseDTOList) {
        checkAgentsExecutor.submit(() -> processHostCheck(hostResponseDTO));
      }

      checkAgentsExecutor.shutdown();
      if (!checkAgentsExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
        log.warn("HostService - Agent check timed out. Stopping check agents executor...");
        checkAgentsExecutor.shutdownNow();
      }

      log.debug("checkAgents() : {}", hostResponseDTOList);

      log.debug("HostService - Agent check for all hosts finished!");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.debug("HostService - Agent check stopped: {}", e.getMessage());
    } catch (Exception e) {
      log.debug("HostService - Agent check failed: {}", e.getMessage(), e);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processHostCheck(HostResponseDTO hostResponseDTO) {
    HostResponseDTO processingHostResponseDTO = processingHost.get(hostResponseDTO.getId());

    if (processingHostResponseDTO != null) {
      return;
    }

    processingHost.put(hostResponseDTO.getId(), hostResponseDTO);

    HostEntity host = hostMapper.fromResponseDTO(hostResponseDTO);
    String monitoringAgentVersion = "";
    String logAgentVersion = "";
    String ip = host.getIp();
    int port = host.getPort();

    try {
      log.debug("Checking host: {}:{}", ip, port);

      HostDTO hostDTO = hostService.findById(host.getId());
      host = hostDTO.toEntity();
      HostConnectionDTO hostConnectionDTO = hostService.getHostConnectionInfo(host.getId());

      String hostname = agentHealthCheckService.getHostname(hostConnectionDTO);
      HostStatus hostStatus = agentHealthCheckService.getHostStatus(hostConnectionDTO.getIp(),
          hostConnectionDTO.getPort());
      String telegrafStatus = AgentServiceStatus.FAILED.toString();
      String fluentBitStatus = AgentServiceStatus.FAILED.toString();

      if (hostStatus == HostStatus.RUNNING) {
        telegrafStatus = agentHealthCheckService.getAgentServiceStatus(Agent.TELEGRAF, host,
            hostConnectionDTO);
        fluentBitStatus = agentHealthCheckService.getAgentServiceStatus(Agent.FLUENT_BIT, host,
            hostConnectionDTO);

        if (!Objects.equals(telegrafStatus, AgentServiceStatus.NOT_EXIST.toString())) {
          monitoringAgentVersion = agentHealthCheckService.getMonitoringAgentVersion(
              hostConnectionDTO);
        }

        if (!Objects.equals(fluentBitStatus, AgentServiceStatus.NOT_EXIST.toString())) {
          logAgentVersion = agentHealthCheckService.getLogAgentVersion(hostConnectionDTO);
        }

        if (!Objects.equals(telegrafStatus, AgentServiceStatus.NOT_EXIST.toString()) ||
                !Objects.equals(fluentBitStatus, AgentServiceStatus.NOT_EXIST.toString())) {
          agentHealthCheckService.writeUnixTimeDiff(hostConnectionDTO);
        }

      }

      log.debug("Host {}:{} ({}) - Status: {}, Telegraf: {}, FluentBit: {}", ip, port, hostname,
          hostStatus, telegrafStatus, fluentBitStatus);
      log.debug("Host {}:{} ({}) - Telegraf Version: {}, FluentBit Version: {}", ip, port, hostname,
              monitoringAgentVersion, logAgentVersion);

      boolean needUpdate = false;

      if (host.getHostname() == null || !host.getHostname().equals(hostname)) {
        needUpdate = true;
        host.setHostname(hostname);
      }
      if (host.getMonitoring_agent_version() == null || !host.getMonitoring_agent_version()
          .equals(monitoringAgentVersion)) {
        needUpdate = true;
        host.setMonitoring_agent_version(monitoringAgentVersion);
      }
      if (host.getLog_agent_version() == null || !host.getLog_agent_version()
          .equals(logAgentVersion)) {
        needUpdate = true;
        host.setLog_agent_version(logAgentVersion);
      }
      if (host.getHost_status() == null || !host.getHost_status().equals(hostStatus)) {
        needUpdate = true;
        host.setHost_status(hostStatus);
      }
      if (host.getMonitoringServiceStatus() == null || !host.getMonitoringServiceStatus()
          .equals(telegrafStatus)) {
        needUpdate = true;
        host.setMonitoringServiceStatus(telegrafStatus);
      }

      if (host.getLogServiceStatus() == null || !host.getLogServiceStatus()
          .equals(fluentBitStatus)) {
        needUpdate = true;
        host.setLogServiceStatus(fluentBitStatus);
      }

      if (needUpdate) {
        hostDomainService.updateHost(host);
      }
    } catch (Exception e) {
      log.debug("Error checking host {}:{} - {}", ip, port, e.getMessage());
    } finally {
      processingHost.remove(hostResponseDTO.getId());
    }
  }

  public List<HostResponseDTO> listHostRefresh() {
    List<HostResponseDTO> hostResponseList = new ArrayList<>();

    checkAgents();

    List<HostEntity> hostList = hostDomainService.listHost();

    for (HostEntity host : hostList) {
      hostResponseList.add(hostMapper.toDTO(host));
    }

    return hostResponseList;
  }


  public List<ResultDTO> delete(HostIDsDTO request) {
    List<ResultDTO> results = new ArrayList<>();

    for (String hostId : CheckUtil.emptyIfNull(List.of(request.getHost_id_list()))) {
      ReentrantLock hostLock = getHostLock(hostId);

      try {
        hostLock.lock();

        // 1) 호스트 존재 확인
        hostService.findById(hostId);

        // 2) 폴더 삭제
        fileService.deleteDirectoryByHostId(hostId);

        // 3) host 삭제
        hostService.deleteById(hostId);

        // 4) 이벤트 발행
        event.publishEvent(
            new AgentHistoryEvent(
                requestInfo.getRequestId(),
                AgentAction.HOST_DELETED,
                hostId,
                requestInfo.getRequestUserId(),
                ""
            )
        );

        // 5) 리턴 값 추가
        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.SUCCESS)
            .build());


      } catch (Exception e) {
        // 실패
        AgentHistoryFailEvent failureEvent = new AgentHistoryFailEvent(
            requestInfo.getRequestId(),
            AgentAction.HOST_UPDATED,
            hostId,
            requestInfo.getRequestUserId(),
            e.getMessage()
        );
        event.publishEvent(failureEvent);

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      } finally {
        hostLock.unlock();
      }
    }

    return results;
  }

  public HostStatisticsResponseDTO hostStatistics() {

    HostStatisticsResponseDTO hostStatisticsResponseDTO = new HostStatisticsResponseDTO();

    int running = 0;
    int monitoringAgentInstalled = 0;
    int logAgentInstalled = 0;

    List<HostEntity> hostList = hostService.list().stream().map(HostDTO::toEntity).toList();
    int registeredTotal = hostList.size();

    for (HostEntity host : hostList) {
      if (host.getHost_status().equals(HostStatus.RUNNING)) {
        running++;
      }
      if (!host.getMonitoringServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
        monitoringAgentInstalled++;
      }
      if (!host.getLogServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
        logAgentInstalled++;
      }
    }

    hostStatisticsResponseDTO.setRunning(running);
    hostStatisticsResponseDTO.setRegisteredTotal(registeredTotal);
    hostStatisticsResponseDTO.setFailed(registeredTotal - running);
    hostStatisticsResponseDTO.setMonitoringAgentInstalled(monitoringAgentInstalled);
    hostStatisticsResponseDTO.setMonitoringAgentNotInstalled(
        registeredTotal - monitoringAgentInstalled);
    hostStatisticsResponseDTO.setLogAgentInstalled(logAgentInstalled);
    hostStatisticsResponseDTO.setLogAgentNotInstalled(registeredTotal - logAgentInstalled);

    return hostStatisticsResponseDTO;
  }
}
