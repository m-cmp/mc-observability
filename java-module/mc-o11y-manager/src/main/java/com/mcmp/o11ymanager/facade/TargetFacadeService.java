package com.mcmp.o11ymanager.facade;


import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.model.host.TargetStatus;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetFacadeService {

  private final TargetService targetService;
  private final AgentFacadeService agentFacadeService;
  private final TumblebugService tumblebugService;



  public TargetDTO postTarget(String nsId, String mciId, String targetId, TargetRegisterDTO dto) {

    TargetStatus status = tumblebugService.isConnectedVM(nsId, mciId, targetId, "cb-user")
        ? TargetStatus.RUNNING
        : TargetStatus.FAILED;

    TargetDTO savedTarget = targetService.post(nsId, mciId, targetId, status, dto);

    if (status == TargetStatus.RUNNING) {
      agentFacadeService.install(nsId, mciId, targetId);
    }

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

  public TargetDTO putTarget(String targetId, String nsId, String mciId, TargetUpdateDTO dto) {
    return targetService.put(targetId, nsId, mciId, dto);
  }

  public void deleteTarget(String targetId, String nsId, String mciId) {
    targetService.delete(targetId, nsId, mciId);
  }
}
