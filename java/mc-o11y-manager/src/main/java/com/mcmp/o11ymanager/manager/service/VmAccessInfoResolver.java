package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.dto.SpiderVm;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfra;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;
import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.infrastructure.spider.SpiderClient;
import com.mcmp.o11ymanager.manager.port.TumblebugPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tumblebug/Spider 응답에서 VM access info를 조립하는 공통 컴포넌트.
 *
 * <p>{@code BeylaController}(Linux)와 {@code OtelJavaController}(Windows)가 동일한 IP/credential 해석 로직을
 * 공유하기 위해 별도 service로 추출. OS별 분기는 호출자가 책임지고, 본 클래스는 단순히 "Tumblebug Node가 주어졌을 때 Ansible로 보낼
 * AccessInfoDTO를 만들어준다"는 한 가지 책임을 가짐.
 *
 * <p><b>IP 해석 정책:</b> {@code publicIP} 우선, 비어 있으면 Spider raw API로 보강 시도, 그것도 실패하면 {@code privateIP}
 * 사용. v0.12.9 Tumblebug가 OpenStack VM의 multi-IP 일부를 응답에서 누락하는 케이스를 우회.
 *
 * <p><b>인증 정책:</b>
 *
 * <ul>
 *   <li>Linux: Tumblebug의 별도 리소스 {@code /resources/sshKey/{id}}에서 private key 조회
 *   <li>Windows: {@code node.nodeUserPassword} (응답에 살아 있을 때만; cloud-init 완료 후 마스킹됨) 사용자 이름은 {@code
 *       node.nodeUserName}이 채워져 있으면 그 값(예: "cb-user") 그대로, 아니면 "Administrator" fallback
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VmAccessInfoResolver {

    private final TumblebugPort tumblebugPort;
    private final SpiderClient spiderClient;

    @Value("${otel-java-agent.winrm-port:5985}")
    private int winrmPort;

    @Value("${otel-java-agent.winrm-scheme:http}")
    private String winrmScheme;

    /** 호출자: Linux/Windows 모두. 본 메서드는 node.isWindows() 기준으로 내부 분기. */
    public AccessInfoDTO resolve(String nsId, String infraId, String nodeId) {
        TumblebugInfra.Node node = tumblebugPort.getNode(nsId, infraId, nodeId);

        // 매핑 검증 로그 — Windows password가 cloud-init 후 누락되는 케이스 추적용. 정상화 후 제거.
        log.info(
                "[VmAccessInfoResolver] node raw: id={} name={} uid={} cspResourceName={} "
                        + "publicIP=[{}] privateIP=[{}] sshPort={} nodeUserName=[{}] "
                        + "nodeUserPassword.isNull={} connectionName={} isWindows={}",
                node.getId(),
                node.getName(),
                node.getUid(),
                node.getCspResourceName(),
                node.getPublicIP(),
                node.getPrivateIP(),
                node.getSshPort(),
                node.getNodeUserName(),
                node.getNodeUserPassword() == null,
                node.getConnectionName(),
                node.isWindows());

        String effectiveIp = resolveReachableIp(node);
        if (effectiveIp == null || effectiveIp.isEmpty()) {
            log.warn(
                    "Node has no reachable IP (both publicIP and privateIP empty): {}/{}/{}",
                    nsId,
                    infraId,
                    nodeId);
            throw new RuntimeException(
                    "Node has no reachable IP. Ensure publicIP or privateIP is set in Tumblebug.");
        }

        if (node.isWindows()) {
            return resolveWindowsAccessInfo(nsId, infraId, nodeId, node, effectiveIp);
        }
        return resolveLinuxAccessInfo(nsId, node, effectiveIp);
    }

    /** Windows 분기. winrm 인증을 위한 password/user/port/scheme 셋업. */
    private AccessInfoDTO resolveWindowsAccessInfo(
            String nsId,
            String infraId,
            String nodeId,
            TumblebugInfra.Node node,
            String effectiveIp) {
        if (node.getNodeUserPassword() == null || node.getNodeUserPassword().isEmpty()) {
            log.warn(
                    "Windows node password not found in Tumblebug response: {}/{}/{}",
                    nsId,
                    infraId,
                    nodeId);
            throw new RuntimeException(
                    "Windows node password not found. Ensure node provisioning makes"
                            + " nodeUserPassword available.");
        }

        // Windows admin user: Tumblebug가 nodeUserName을 채워주면 그 값 사용, 아니면 OS 표준 fallback.
        String winUser =
                node.getNodeUserName() != null && !node.getNodeUserName().isEmpty()
                        ? node.getNodeUserName()
                        : "Administrator";

        return AccessInfoDTO.builder()
                .ip(effectiveIp)
                // Tumblebug 응답의 sshPort=22를 그대로 쓰면 winrm 안 됨. yaml 설정값으로 override.
                .port(winrmPort)
                .user(winUser)
                .password(node.getNodeUserPassword())
                .osType("windows")
                .winrmScheme(winrmScheme)
                .build();
    }

    /** Linux 분기. SSH key 인증. private key는 Tumblebug 별도 리소스에서 조회. */
    private AccessInfoDTO resolveLinuxAccessInfo(
            String nsId, TumblebugInfra.Node node, String effectiveIp) {
        TumblebugSshKey sshKey = tumblebugPort.getSshKey(nsId, node.getSshKeyId());
        if (sshKey == null) {
            log.warn("SSH private key not found");
            throw new RuntimeException("SSH private key not found");
        }
        return AccessInfoDTO.builder()
                .ip(effectiveIp)
                .port(Integer.parseInt(node.getSshPort()))
                .user(node.getNodeUserName())
                .sshKey(sshKey.getPrivateKey())
                .osType("linux")
                .build();
    }

    /**
     * Tumblebug publicIP → Spider raw publicIP → Tumblebug privateIP 순서로 fallback.
     *
     * <p>두 번째 단계는 v0.12.9 Tumblebug가 OpenStack VM의 multi-IP 중 일부만 publicIP로 surface하는 케이스를 풀어주기 위함.
     * Spider raw 응답엔 OpenStack 원본 정보가 들어있다.
     */
    private String resolveReachableIp(TumblebugInfra.Node node) {
        if (node.getPublicIP() != null && !node.getPublicIP().isEmpty()) {
            return node.getPublicIP();
        }
        String spiderIp = trySpiderPublicIp(node);
        if (spiderIp != null && !spiderIp.isEmpty()) {
            return spiderIp;
        }
        return node.getPrivateIP();
    }

    /**
     * Spider {@code GET /spider/vm/{name}?ConnectionName=...} 호출로 publicIP 보강. Spider VM 이름이
     * Tumblebug 응답의 어느 필드와 매칭되는지 환경마다 다를 수 있어 cspResourceName → uid → name 순서로 시도. 호출 실패는 catch만 하고
     * null 반환.
     */
    private String trySpiderPublicIp(TumblebugInfra.Node node) {
        if (node.getConnectionName() == null || node.getConnectionName().isEmpty()) {
            return null;
        }
        String[] nameCandidates = {node.getCspResourceName(), node.getUid(), node.getName()};
        for (String name : nameCandidates) {
            if (name == null || name.isEmpty()) continue;
            try {
                SpiderVm sp = spiderClient.getVm(name, node.getConnectionName());
                if (sp != null && sp.getPublicIP() != null && !sp.getPublicIP().isEmpty()) {
                    log.info(
                            "Resolved Spider PublicIP via name='{}' (conn={}): {}",
                            name,
                            node.getConnectionName(),
                            sp.getPublicIP());
                    return sp.getPublicIP();
                }
            } catch (Exception e) {
                log.debug(
                        "Spider getVm failed for name='{}' (conn={}): {}",
                        name,
                        node.getConnectionName(),
                        e.getMessage());
            }
        }
        return null;
    }

    /** OS 판단을 분리할 일이 있을 때를 위한 헬퍼 (예: controller 단에서 가드용). */
    public boolean isWindowsNode(String nsId, String infraId, String nodeId) {
        TumblebugInfra.Node node = tumblebugPort.getNode(nsId, infraId, nodeId);
        return node != null && node.isWindows();
    }
}
