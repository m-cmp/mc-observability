package com.mcmp.o11ymanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.exception.agent.AgentStatusException;
import com.mcmp.o11ymanager.port.TumblebugPort;
import com.mcmp.o11ymanager.service.interfaces.TumblebugService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TumblebugServiceImpl implements TumblebugService {

  private final TumblebugPort tumblebugPort;
  private final ObjectMapper objectMapper;

  @Value("${deploy.site-code}")
  private String sitecode;

  @Override
  public String executeCommand(String nsId, String mciId, String targetId, String userName, String command) {
    TumblebugCmd cmd = new TumblebugCmd();
    cmd.setCommand(List.of(command));
    cmd.setUserName(userName);

    Object response = tumblebugPort.sendCommand(nsId, mciId, targetId, cmd);

    try {
      JsonNode root = objectMapper.valueToTree(response);
      JsonNode results = root.path("results");
      if (results.isArray() && results.size() > 0) {
        JsonNode stdout = results.get(0).path("stdout");
        return stdout.path("0").asText();
      }
    } catch (Exception e) {
      log.info("tumblebug cmd error: {}", e.getMessage());
      throw new RuntimeException("Tumblebug 응답 파싱 실패: " + e.getMessage(), e);
    }

    throw new RuntimeException("Tumblebug 응답이 유효하지 않습니다.");
  }


  @Override
  public boolean isConnectedVM(String nsId, String mciId, String targetId, String userName) {
    try {
      String output = executeCommand(nsId, mciId, targetId, userName, "echo hello");
      return "hello".equalsIgnoreCase(output.trim());
    } catch (Exception e) {
      return false;
    }
  }


  @Override
  public TumblebugMCI.Vm getVm(String nsId, String mciId, String targetId) {
    return tumblebugPort.getVM(nsId, mciId, targetId);
  }


  @Override
    public boolean isServiceActive(String nsId, String mciId, String targetId, String userName, Agent agent) {
    log.info(">>> isServiceActive called with nsId: {}, mciId: {}, agent: {}", nsId, mciId, agent);
    String command = String.format("systemctl is-active cmp-%s-%s.service", agent.name().toLowerCase().replace("_", "-"), sitecode.toLowerCase());

    log.info("==================IS ACTIVE ? INACTIVE CMD : {}", command);


    String result = executeCommand(nsId, mciId, targetId, userName, command);
    String trimmed = result.trim();

    log.info("===============================================Tumblebug Command Result : '{}'===============================================", trimmed);

    if (!"active".equalsIgnoreCase(trimmed)) {
      log.info("===============================================Agent Status Failed gent: {}, result: {}===============================================", agent, trimmed);
      throw new AgentStatusException("systemctl-check", "Agent 상태가 비정상입니다", agent);
    }

    return true;
  }


}
