package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.port.TumblebugPort;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetFacadeService {

  private final TargetService targetService;
  private final TumblebugPort tumblebugPort;
  private final AgentFacadeService agentFacadeService;

  public TargetDTO postTarget(String nsId, String mciId, String targetId, TargetRegisterDTO dto) {

    TumblebugCmd cmd = new TumblebugCmd();
    cmd.setCommand(Arrays.asList("echo hello"));
    cmd.setUserName("cb-user");

    try {
      Map<String, Object> responseMap = (Map<String, Object>) tumblebugPort.sendCommand(nsId, mciId, cmd);
      List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");

      if (results == null || results.isEmpty()) {
        throw new RuntimeException("Result is empty");
      }

      Map<String, Object> firstResult = results.get(0);

      if (firstResult == null) {
        throw new RuntimeException("first result is null.");
      }

      Map<String, String> stdoutMap = (Map<String, String>) firstResult.get("stdout");

      if (stdoutMap == null || !stdoutMap.containsKey("0")) {
        throw new RuntimeException("Result is empty or not in the expected format no '0' key");
      }

      String actualOutput = stdoutMap.get("0");

      if (actualOutput == null || !"hello".equals(actualOutput.trim())) {
        throw new RuntimeException("Response is not hello. Actual Response: '" + actualOutput + "'");
      }
    } catch (Exception e) {
      throw new RuntimeException("Connection Error: " + e.getMessage(), e);
    }

    TargetDTO savedTarget = targetService.post(nsId, mciId, targetId, dto);

    log.info("=============================save target db=================================");

    agentFacadeService.install(nsId, mciId, targetId);

    return savedTarget;
  }

  public TargetDTO getTarget(String nsId, String mciId, String targetId) {
    TumblebugMCI.Vm vm = tumblebugPort.getVM(nsId, mciId, targetId);

    return TargetDTO.builder()
        .id(vm.getId())
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
