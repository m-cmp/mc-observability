package com.mcmp.o11ymanager.facade;


import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.global.error.ResourceNotExistsException;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetStatus;
import com.mcmp.o11ymanager.repository.TargetJpaRepository;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import java.util.List;

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

  // TODO : Agent Task Status : IDLE
  @Transactional
  public TargetDTO updateTargetTaskStatuses(String nsId, String mciId, String targetId, TargetAgentTaskStatus monitoringStatus, TargetAgentTaskStatus logStatus) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(() -> new ResourceNotExistsException("...", "TargetEntity", targetId));

    target.setMonitoringAgentTaskStatus(monitoringStatus);
    target.setLogAgentTaskStatus(logStatus);

    TargetEntity updatedEntity = targetJpaRepository.save(target);

    return TargetDTO.fromEntity(updatedEntity);
  }


  @Transactional
  public TargetDTO postTarget(String nsId, String mciId, String targetId, TargetRequestDTO dto) {

    TargetStatus status = tumblebugService.isConnectedVM(nsId, mciId, targetId, "cb-user")
        ? TargetStatus.RUNNING
        : TargetStatus.FAILED;

    TargetDTO savedTarget = targetService.post(nsId, mciId, targetId, status, dto);

    updateTargetTaskStatuses(
        nsId, mciId, targetId,
        TargetAgentTaskStatus.IDLE, // 모니터링 에이전트 상태를 IDLE로
        TargetAgentTaskStatus.IDLE  // 로그 에이전트 상태를 IDLE로
    );

//    if (status == TargetStatus.RUNNING) {
//      agentFacadeService.install(nsId, mciId, targetId);
//    }

    agentFacadeService.install(nsId, mciId, targetId);

    return savedTarget;
  }



  public TargetDTO getTarget(String nsId, String mciId, String targetId) {
    TumblebugMCI.Vm vm = tumblebugService.getVm(nsId, mciId, targetId);

    return TargetDTO.builder()
        .targetId(vm.getId())
        .name(vm.getName())
        .aliasName(vm.getAliasName())
        .description(vm.getDescription())
        .nsId(nsId)
        .mciId(mciId)
        .state(vm.getState())
        .build();
  }


  public List<TargetDTO> getTargetsNsMci(String nsId, String mciId) {
    return targetService.getByNsMci(nsId, mciId);
  }

  public List<TargetDTO> getTargets() {
    return targetService.list();
  }

  public TargetDTO putTarget(String nsId, String mciId, String targetId, TargetRequestDTO dto) {
    return targetService.put(nsId, mciId, targetId, dto);
  }

  public void deleteTarget(String nsId, String mciId, String targetId) {
    targetService.delete(nsId, mciId, targetId);
  }
}
