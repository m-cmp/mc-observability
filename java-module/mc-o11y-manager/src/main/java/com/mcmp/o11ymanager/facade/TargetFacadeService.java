package com.mcmp.o11ymanager.facade;


import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetStatus;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetFacadeService {


  private ExecutorService executor;

  private final TargetService targetService;
  private final AgentFacadeService agentFacadeService;
  private final TumblebugService tumblebugService;


  @Transactional
  public TargetDTO postTarget(String nsId, String mciId, String targetId, TargetRequestDTO dto) {

    TargetDTO savedTarget;
    TargetStatus status = tumblebugService.isConnectedVM(nsId, mciId, targetId)
        ? TargetStatus.RUNNING
        : TargetStatus.FAILED;

    if (status == TargetStatus.RUNNING) {
      savedTarget = targetService.post(nsId, mciId, targetId, status, dto);
    } else {
      throw new RuntimeException("FAILED TO CONNECT VM");
    }

    targetService.updateMonitoringAgentTaskStatusAndTaskId(savedTarget.getNsId(),
        savedTarget.getMciId(), savedTarget.getTargetId(), TargetAgentTaskStatus.IDLE, "");
    targetService.updateLogAgentTaskStatusAndTaskId(savedTarget.getNsId(), savedTarget.getMciId(),
        savedTarget.getTargetId(), TargetAgentTaskStatus.IDLE, "");

    agentFacadeService.install(nsId, mciId, targetId);

    log.info(">>> start checking monitoring agent status");
    AgentServiceStatus monitoringStatus = agentFacadeService.getAgentServiceStatus(nsId, mciId,
        targetId, Agent.TELEGRAF);
    log.info(">>> start checking log agent status");
    AgentServiceStatus logStatus = agentFacadeService.getAgentServiceStatus(nsId, mciId, targetId,
        Agent.FLUENT_BIT);

    savedTarget.setMonitoringServiceStatus(monitoringStatus);
    savedTarget.setLogServiceStatus(logStatus);

    return savedTarget;
  }


  public TargetDTO getTarget(String nsId, String mciId, String targetId) {
    log.info(">>> getTarget() called with nsId: {}, mciId: {}, targetId: {}", nsId, mciId,
        targetId);

    TumblebugMCI.Vm vm;
    String userName;

    TargetDTO savedTarget = targetService.get(nsId, mciId, targetId);

    try {
      vm = tumblebugService.getVm(nsId, mciId, targetId);
      userName = vm.getVmUserName();
      log.info(">>> VM fetched: id={}, name={} userName={}", vm.getId(), vm.getName(), userName);
    } catch (Exception e) {
      log.error(">>> getVm() failed", e);
      throw e;
    }

    log.info(">>> start checking monitoring agent status");
    AgentServiceStatus monitoringStatus = agentFacadeService.getAgentServiceStatus(nsId, mciId,
        targetId, Agent.TELEGRAF);
    log.info(">>> start checking log agent status");
    AgentServiceStatus logStatus = agentFacadeService.getAgentServiceStatus(nsId, mciId, targetId,
        Agent.FLUENT_BIT);

    return savedTarget.builder()
        .targetId(vm.getId())
        .name(savedTarget.getName())
        .description(vm.getDescription())
        .nsId(nsId)
        .mciId(mciId)
        .monitoringServiceStatus(monitoringStatus)
        .logServiceStatus(logStatus)
        .targetStatus(savedTarget.getTargetStatus())
        .build();
  }


  private List<TargetDTO> fetchTarget(List<TargetDTO> rawList) {
    List<Future<TargetDTO>> futures = new ArrayList<>();

    for (TargetDTO baseDto : rawList) {
      futures.add(executor.submit(() -> {
        try {
          return getTarget(baseDto.getNsId(), baseDto.getMciId(), baseDto.getTargetId());
        } catch (Exception e) {
          log.error(">>> getTarget() failed for: nsId={}, mciId={}, targetId={}",
              baseDto.getNsId(), baseDto.getMciId(), baseDto.getTargetId(), e);
          return null;
        }
      }));
    }

    List<TargetDTO> result = new ArrayList<>();
    for (Future<TargetDTO> future : futures) {
      try {
        TargetDTO dto = future.get();
        if (dto != null) {
          result.add(dto);
        }
      } catch (Exception e) {
        log.error(">>> future.get() failed", e);
      }
    }

    return result;
  }


  public List<TargetDTO> getTargetsNsMci(String nsId, String mciId) {

    List<TargetDTO> rawList = targetService.getByNsMci(nsId, mciId);

    return fetchTarget(rawList);

  }


  public List<TargetDTO> getTargets() {
    List<TargetDTO> rawList = targetService.list();
    return fetchTarget(rawList);
  }


  public TargetDTO putTarget(String nsId, String mciId, String targetId, TargetRequestDTO dto) {
    return targetService.put(nsId, mciId, targetId, dto);
  }

  public void deleteTarget(String nsId, String mciId, String targetId) {
    targetService.delete(nsId, mciId, targetId);
  }

}
