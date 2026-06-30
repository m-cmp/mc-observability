package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfra;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.host.VMStatus;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VMFacadeService {

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        this.executor = Executors.newFixedThreadPool(10);
    }

    private final VMService vmService;
    private final AgentFacadeService agentFacadeService;
    private final TumblebugService tumblebugService;
    private final InfluxDbService influxDbService;

    public VMDTO postVM(String nsId, String infraId, String nodeId, VMRequestDTO dto) {

        ReentrantLock hostLock = vmService.getHostLock(nsId, infraId, nodeId);

        try {
            hostLock.lock();

            VMStatus status =
                    tumblebugService.isConnectedVM(nsId, infraId, nodeId)
                            ? VMStatus.RUNNING
                            : VMStatus.FAILED;

            if (status != VMStatus.RUNNING) {
                throw new IllegalArgumentException(
                        "Cannot connect to the VM — it must be running and reachable to"
                                + " install/uninstall an agent. Start the VM and try again.");
            }

            Long influxSeq = influxDbService.resolveInfluxDb(nsId, infraId);

            VMDTO savedVM = vmService.post(nsId, infraId, nodeId, status, dto, influxSeq);

            vmService.updateMonitoringAgentTaskStatusAndTaskId(
                    savedVM.getNsId(),
                    savedVM.getInfraId(),
                    savedVM.getNodeId(),
                    VMAgentTaskStatus.IDLE,
                    "");
            vmService.updateLogAgentTaskStatusAndTaskId(
                    savedVM.getNsId(),
                    savedVM.getInfraId(),
                    savedVM.getNodeId(),
                    VMAgentTaskStatus.IDLE,
                    "");
            vmService.updateTraceAgentTaskStatusAndTaskId(
                    savedVM.getNsId(),
                    savedVM.getInfraId(),
                    savedVM.getNodeId(),
                    VMAgentTaskStatus.IDLE,
                    "");

            // dto.gpu=true면 DCGM Exporter 설치 + telegraf GPU(dcgm) 수집 설정 포함
            agentFacadeService.install(nsId, infraId, nodeId, Boolean.TRUE.equals(dto.getGpu()));

            return savedVM;
        } finally {
            hostLock.unlock();
        }
    }

    // --- Per-agent install / uninstall (monitoring = telegraf, log = fluent-bit) --------------
    // VM monitoring and log agents can be managed independently, like K8s nodes. The first
    // install registers the node; the node record stays even when both agents are uninstalled
    // (both then show NOT_INSTALLED -> Install button).

    public List<ResultDTO> installMonitoringAgent(String nsId, String infraId, String nodeId) {
        ReentrantLock hostLock = vmService.getHostLock(nsId, infraId, nodeId);
        try {
            hostLock.lock();
            ensureRegistered(nsId, infraId, nodeId, Agent.TELEGRAF);
            return agentFacadeService.installMonitoringAgent(nsId, infraId, nodeId);
        } finally {
            hostLock.unlock();
        }
    }

    public List<ResultDTO> uninstallMonitoringAgent(String nsId, String infraId, String nodeId) {
        return agentFacadeService.uninstallMonitoringAgent(nsId, infraId, nodeId);
    }

    public List<ResultDTO> installLogAgent(String nsId, String infraId, String nodeId) {
        ReentrantLock hostLock = vmService.getHostLock(nsId, infraId, nodeId);
        try {
            hostLock.lock();
            ensureRegistered(nsId, infraId, nodeId, Agent.FLUENT_BIT);
            return agentFacadeService.installLogAgent(nsId, infraId, nodeId);
        } finally {
            hostLock.unlock();
        }
    }

    public List<ResultDTO> uninstallLogAgent(String nsId, String infraId, String nodeId) {
        return agentFacadeService.uninstallLogAgent(nsId, infraId, nodeId);
    }

    private void ensureRegistered(String nsId, String infraId, String nodeId, Agent installing) {
        boolean exists;
        try {
            // Must check by the full (nsId, infraId, nodeId) key, not (nsId, nodeId): node ids are
            // only unique within an MCI, so different MCIs routinely share one (e.g. every
            // cluster's
            // first node is "n1-1"). A (nsId, nodeId) lookup matches a *different* MCI's node, so
            // we
            // wrongly conclude this node is already registered, skip the insert, and the subsequent
            // agent install fails with "VMEntity ID does not exist".
            vmService.get(nsId, infraId, nodeId);
            exists = true;
        } catch (Exception e) {
            exists = false;
        }
        if (exists) {
            return;
        }

        VMStatus status =
                tumblebugService.isConnectedVM(nsId, infraId, nodeId)
                        ? VMStatus.RUNNING
                        : VMStatus.FAILED;
        if (status != VMStatus.RUNNING) {
            throw new IllegalArgumentException(
                    "Cannot connect to the VM — it must be running and reachable to"
                            + " install/uninstall an agent. Start the VM and try again.");
        }
        Long influxSeq = influxDbService.resolveInfluxDb(nsId, infraId);
        VMRequestDTO dto = VMRequestDTO.builder().name(nodeId).build();
        vmService.post(nsId, infraId, nodeId, status, dto, influxSeq);

        // Agent being installed -> IDLE (the install flow moves it to INSTALLING); the others
        // start NOT_INSTALLED so the UI shows them as installable.
        vmService.updateMonitoringAgentTaskStatusAndTaskId(
                nsId,
                infraId,
                nodeId,
                installing == Agent.TELEGRAF
                        ? VMAgentTaskStatus.IDLE
                        : VMAgentTaskStatus.NOT_INSTALLED,
                "");
        vmService.updateLogAgentTaskStatusAndTaskId(
                nsId,
                infraId,
                nodeId,
                installing == Agent.FLUENT_BIT
                        ? VMAgentTaskStatus.IDLE
                        : VMAgentTaskStatus.NOT_INSTALLED,
                "");
        vmService.updateTraceAgentTaskStatusAndTaskId(
                nsId, infraId, nodeId, VMAgentTaskStatus.NOT_INSTALLED, "");
    }

    public VMDTO getVM(String nsId, String infraId, String nodeId) {

        log.info(
                ">>> getVM() called with nsId: {}, infraId: {}, nodeId: {}", nsId, infraId, nodeId);

        ReentrantLock hostLock = vmService.getHostLock(nsId, infraId, nodeId);

        try {
            hostLock.lock();

            VMDTO savedVM;
            try {
                savedVM = vmService.get(nsId, infraId, nodeId);
            } catch (Exception notRegistered) {
                // The node exists in cb-tumblebug but was never registered for monitoring (no
                // agent installed yet). Don't 500 here — the monitoring-config page polls this for
                // every tumblebug node, and a hard error makes the UI retry in a tight loop and
                // pile requests up. Report the node as installable (NOT_INSTALLED) so the page can
                // render an Install button. Display name is best-effort from tumblebug.
                String name = nodeId;
                try {
                    name = tumblebugService.getNode(nsId, infraId, nodeId).getName();
                } catch (Exception ignore) {
                    // tumblebug node lookup is best-effort for the label only
                }
                return VMDTO.builder()
                        .nodeId(nodeId)
                        .name(name != null ? name : nodeId)
                        .nsId(nsId)
                        .infraId(infraId)
                        .monitoringAgentStatus(AgentStatus.NOT_INSTALLED)
                        .logAgentStatus(AgentStatus.NOT_INSTALLED)
                        .traceAgentStatus(AgentStatus.NOT_INSTALLED)
                        .build();
            }
            TumblebugInfra.Node node = tumblebugService.getNode(nsId, infraId, nodeId);
            String userName = node.getNodeUserName();
            log.info(
                    ">>> VM fetched: id={}, name={} userName={}",
                    node.getId(),
                    node.getName(),
                    userName);

            //        log.info(">>> start checking monitoring agent status");
            //        AgentServiceStatus monitoringStatus =
            //                agentFacadeService.getAgentServiceStatus(nsId, infraId, nodeId,
            // Agent.TELEGRAF);
            //        log.info(">>> start checking log agent status");
            //        AgentServiceStatus logStatus =
            //                agentFacadeService.getAgentServiceStatus(nsId, infraId, nodeId,
            // Agent.FLUENT_BIT);

            AgentStatus monitoringAgentStatus =
                    agentFacadeService.getAgentStatus(nsId, infraId, nodeId, Agent.TELEGRAF);

            AgentStatus logAgentStatus =
                    agentFacadeService.getAgentStatus(nsId, infraId, nodeId, Agent.FLUENT_BIT);

            AgentStatus traceAgentStatus =
                    agentFacadeService.getAgentStatus(nsId, infraId, nodeId, Agent.BEYLA);

            return VMDTO.builder()
                    .nodeId(node.getId())
                    .name(savedVM.getName())
                    .description(node.getDescription())
                    .nsId(nsId)
                    .infraId(infraId)
                    .monitoringAgentStatus(monitoringAgentStatus)
                    .logAgentStatus(logAgentStatus)
                    .traceAgentStatus(traceAgentStatus)
                    .build();
        } catch (Exception e) {
            log.error(">>> getVM() failed", e);
            throw e;
        } finally {
            hostLock.unlock();
        }
    }

    private List<VMDTO> fetchVM(List<VMDTO> rawList) {

        List<Future<VMDTO>> futures = new ArrayList<>();

        for (VMDTO baseDto : rawList) {
            futures.add(
                    executor.submit(
                            () -> {
                                try {
                                    return getVM(
                                            baseDto.getNsId(),
                                            baseDto.getInfraId(),
                                            baseDto.getNodeId());
                                } catch (Exception e) {
                                    log.error(
                                            ">>> getVM() failed for: nsId={}, infraId={}, nodeId={}",
                                            baseDto.getNsId(),
                                            baseDto.getInfraId(),
                                            baseDto.getNodeId(),
                                            e);
                                    return null;
                                }
                            }));
        }

        List<VMDTO> result = new ArrayList<>();
        for (Future<VMDTO> future : futures) {
            try {
                VMDTO dto = future.get();
                if (dto != null) {
                    result.add(dto);
                }
            } catch (Exception e) {
                log.error(">>> future.get() failed", e);
            }
        }

        return result;
    }

    public List<VMDTO> getVMsNsMci(String nsId, String infraId) {

        List<VMDTO> rawList = vmService.getByNsMci(nsId, infraId);

        return fetchVM(rawList);
    }

    public List<VMDTO> getVMs() {

        List<VMDTO> rawList = vmService.list();

        return fetchVM(rawList);
    }

    public VMDTO putVM(String nsId, String infraId, String nodeId, VMRequestDTO dto) {

        ReentrantLock hostLock = vmService.getHostLock(nsId, infraId, nodeId);

        try {
            hostLock.lock();

            return vmService.put(nsId, infraId, nodeId, dto);
        } finally {
            hostLock.unlock();
        }
    }

    public void deleteVM(String nsId, String infraId, String nodeId) {

        ReentrantLock hostLock = vmService.getHostLock(nsId, infraId, nodeId);

        try {
            hostLock.lock();

            vmService.delete(nsId, infraId, nodeId);
            agentFacadeService.uninstall(nsId, infraId, nodeId);
        } finally {
            hostLock.unlock();
        }
    }
}
