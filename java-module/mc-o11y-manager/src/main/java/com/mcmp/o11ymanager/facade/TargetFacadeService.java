package com.mcmp.o11ymanager.facade;


import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetStatus;
import com.mcmp.o11ymanager.repository.TargetJpaRepository;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import com.mcmp.o11ymanager.tracing.ExecutorFactory;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.text.html.HTML.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetFacadeService {

  private final TargetService targetService;
  private final AgentFacadeService agentFacadeService;
  private final TumblebugService tumblebugService;
  private final TargetJpaRepository targetJpaRepository;


  @Transactional
  public TargetDTO postTarget(String nsId, String mciId, String targetId, TargetRequestDTO dto) {

    TargetDTO savedTarget;
    TumblebugMCI.Vm vm = tumblebugService.getVm(nsId, mciId, targetId);
    if (vm == null) {
      throw new RuntimeException("FAILED TO GET VM");
    }

    TargetStatus status = tumblebugService.isConnectedVM(nsId, mciId, targetId, vm.getVmUserName())
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

    return savedTarget;
  }



  public TargetDTO getTarget(String nsId, String mciId, String targetId) {
    log.info(">>> getTarget() called with nsId: {}, mciId: {}, targetId: {}", nsId, mciId, targetId);

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
    AgentServiceStatus monitoringStatus = agentFacadeService.getAgentServiceStatus(nsId, mciId, targetId, userName, Agent.TELEGRAF);
    log.info(">>> start checking log agent status");
    AgentServiceStatus logStatus = agentFacadeService.getAgentServiceStatus(nsId, mciId, targetId, userName, Agent.FLUENT_BIT);

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


  public List<TargetDTO> getTargetsNsMci(String nsId, String mciId) {
    return targetService.getByNsMci(nsId, mciId);
  }



  public List<TargetDTO> getTargets() {
    List<TargetDTO> rawList = targetService.list();

    int maxThreads = 10;
    ExecutorService executor = ExecutorFactory.newFixedThreadPool(10);
    List<Future<TargetDTO>> futures = new ArrayList<Future<TargetDTO>>();

    for (TargetDTO baseDto : rawList) {
      futures.add(executor.submit(new Callable<TargetDTO>() {
        @Override
        public TargetDTO call() {
          try {
            String nsId = baseDto.getNsId();
            String mciId = baseDto.getMciId();
            String targetId = baseDto.getTargetId();
            return getTarget(nsId, mciId, targetId);
          } catch (Exception e) {
            log.error(">>> getTarget() failed for: nsId={}, mciId={}, targetId={}",
                baseDto.getNsId(), baseDto.getMciId(), baseDto.getTargetId(), e);
            return null;
          }
        }
      }));
    }

    List<TargetDTO> result = new ArrayList<TargetDTO>();
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

    executor.shutdown();

    return result;
  }




  public List<TargetDTO> getTargets2() {
    return targetService.list();
  }

  public TargetDTO putTarget(String nsId, String mciId, String targetId, TargetRequestDTO dto) {
    return targetService.put(nsId, mciId, targetId, dto);
  }

  public void deleteTarget(String nsId, String mciId, String targetId) {
    targetService.delete(nsId, mciId, targetId);
  }

}
