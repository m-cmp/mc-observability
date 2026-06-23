package com.mcmp.o11ymanager.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugCmd;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfra;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.exception.agent.AgentStatusException;
import com.mcmp.o11ymanager.manager.port.TumblebugPort;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
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
    public String executeCommand(String nsId, String infraId, String nodeId, String command) {
        TumblebugCmd cmd = new TumblebugCmd();
        cmd.setCommand(List.of(command));

        TumblebugInfra.Node node = getNode(nsId, infraId, nodeId);
        if (node == null) {
            throw new RuntimeException("FAILED TO GET NODE");
        }

        cmd.setUserName(node.getNodeUserName());

        Object response = tumblebugPort.sendCommand(nsId, infraId, nodeId, cmd);

        try {
            JsonNode root = objectMapper.valueToTree(response);
            JsonNode results = root.path("results");
            if (results.isArray() && !results.isEmpty()) {
                JsonNode stdout = results.get(0).path("stdout");
                return stdout.path("0").asText();
            }
        } catch (Exception e) {
            log.info("tumblebug cmd error: {}", e.getMessage());
            throw new RuntimeException("Tumblebug response parsing failed: " + e.getMessage(), e);
        }

        throw new RuntimeException("Tumblebug response is invalid.");
    }

    @Override
    public boolean isConnectedVM(String nsId, String infraId, String nodeId) {
        try {
            String output = executeCommand(nsId, infraId, nodeId, "echo hello");
            return "hello".equalsIgnoreCase(output.trim());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public TumblebugInfra.Node getNode(String nsId, String infraId, String nodeId) {
        return tumblebugPort.getNode(nsId, infraId, nodeId);
    }

    @Override
    public boolean isServiceActive(String nsId, String infraId, String nodeId, Agent agent) {
        log.info(
                ">>> isServiceActive called with nsId: {}, infraId: {}, agent: {}",
                nsId,
                infraId,
                agent);

        // Windows OTel Java agent는 systemctl 대상이 아니다. JAVA_TOOL_OPTIONS 환경변수에
        // -javaagent 옵션이 들어있는지 PowerShell로 확인하는 단순 로직으로 대체.
        if (agent == Agent.OTEL_JAVA_AGENT) {
            String winCommand =
                    "powershell -Command \"if ([Environment]::GetEnvironmentVariable('JAVA_TOOL_OPTIONS','Machine') -match '-javaagent') { 'active' } else { 'inactive' }\"";
            log.info("==================OTEL JAVA AGENT IS ACTIVE CMD : {}", winCommand);

            String winResult = executeCommand(nsId, infraId, nodeId, winCommand).trim();
            log.info("OTel Java agent active check result: '{}'", winResult);
            return "active".equalsIgnoreCase(winResult);
        }

        String command =
                String.format(
                        "systemctl is-active cmp-%s-%s.service",
                        agent.name().toLowerCase().replace("_", "-"), sitecode.toLowerCase());

        log.info("==================IS ACTIVE ? INACTIVE CMD : {}", command);

        String result = executeCommand(nsId, infraId, nodeId, command);
        String trimmed = result.trim();

        log.info(
                "===============================================Tumblebug Command Result : '{}'===============================================",
                trimmed);

        if (!"active".equalsIgnoreCase(trimmed)) {
            log.info(
                    "===============================================Agent Status Failed gent: {}, result: {}===============================================",
                    agent,
                    trimmed);
            return false;
        }

        return true;
    }

    @Override
    public String restart(String nsId, String infraId, String nodeId, Agent agent) {

        log.info("=================RESTART AGENT======================");

        // Windows OTel Java agent는 호스트 단위 환경변수 주입 방식이라 별도 service 단위 restart가
        // 의미 없다. POC에선 미지원으로 명확히 throw.
        if (agent == Agent.OTEL_JAVA_AGENT) {
            throw new AgentStatusException(
                    "restart-unsupported",
                    "Restart is not supported for OTel Java Agent (Windows). Restart the target"
                            + " Java application instead.",
                    agent);
        }

        // Must run with sudo: the SSH user (e.g. cb-user) cannot restart a systemd unit
        // otherwise ("Interactive authentication required"), which left the agent inactive
        // after a VM suspend/resume and surfaced as a 500 when toggling a metric.
        String command =
                String.format(
                        "sudo systemctl restart cmp-%s-%s.service",
                        agent.name().toLowerCase().replace("_", "-"), sitecode.toLowerCase());

        String result = executeCommand(nsId, infraId, nodeId, command).trim();

        if (!result.isEmpty()) {
            log.warn("❌ Agent Restart Failed - Agent: {}, Result: {}", agent, result);
            throw new AgentStatusException("systemctl-check", "Agent Restart Failed", agent);
        }

        log.info("✅ Agent Restarted Successfully - Agent: {}", agent);
        return result;
    }
}
