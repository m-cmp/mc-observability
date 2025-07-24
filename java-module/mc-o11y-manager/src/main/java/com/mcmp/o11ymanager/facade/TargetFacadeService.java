package com.mcmp.o11ymanager.facade;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.port.TumblebugPort;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import java.util.List;

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
  private final ObjectMapper objectMapper;


  public String executeCommand(String nsId, String mciId, String command, String userName) {
    TumblebugCmd cmd = new TumblebugCmd();
    cmd.setCommand(List.of(command));
    cmd.setUserName(userName);

    Object response = tumblebugPort.sendCommand(nsId, mciId, cmd);

    try {
      JsonNode root = objectMapper.valueToTree(response);
      JsonNode results = root.path("results");
      if (results.isArray() && results.size() > 0) {
        JsonNode stdout = results.get(0).path("stdout");
        return stdout.path("0").asText();
      }
    } catch (Exception e) {
      throw new RuntimeException("Tumblebug 응답 파싱 실패: " + e.getMessage(), e);
    }

    throw new RuntimeException("Tumblebug 응답이 유효하지 않습니다.");
  }

  public TargetDTO postTarget(String nsId, String mciId, String targetId, TargetRegisterDTO dto) {


    String output;
    try {
      output = executeCommand(nsId, mciId, "echo hello", "cb-user");

      if (!"hello".equals(output.trim())) {
        throw new RuntimeException("Response is not hello. Actual Response: '" + output + "'");
      }

    } catch (Exception e) {
      throw new RuntimeException("Connection Error: " + e.getMessage(), e);
    }

    TargetDTO savedTarget = targetService.post(nsId, mciId, targetId, dto);

    agentFacadeService.install(nsId, mciId, targetId);

    return savedTarget;
  }

  public TargetDTO getTarget(String nsId, String mciId, String targetId) {
    TumblebugMCI.Vm vm = tumblebugPort.getVM(nsId, mciId, targetId);

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
