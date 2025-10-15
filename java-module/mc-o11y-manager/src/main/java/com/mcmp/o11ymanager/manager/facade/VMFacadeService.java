package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentServiceStatus;
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

    public VMDTO postVM(String nsId, String mciId, String vmId, VMRequestDTO dto) {

        VMDTO savedVM;
        VMStatus status =
                tumblebugService.isConnectedVM(nsId, mciId, vmId)
                        ? VMStatus.RUNNING
                        : VMStatus.FAILED;

        if (status != VMStatus.RUNNING) {
            throw new RuntimeException("FAILED TO CONNECT VM");
        }

        Long influxSeq = influxDbService.resolveInfluxDb(nsId, mciId);

        savedVM = vmService.post(nsId, mciId, vmId, status, dto, influxSeq);

        vmService.updateMonitoringAgentTaskStatusAndTaskId(
                savedVM.getNsId(),
                savedVM.getMciId(),
                savedVM.getVmId(),
                VMAgentTaskStatus.IDLE,
                "");
        vmService.updateLogAgentTaskStatusAndTaskId(
                savedVM.getNsId(),
                savedVM.getMciId(),
                savedVM.getVmId(),
                VMAgentTaskStatus.IDLE,
                "");

        agentFacadeService.install(nsId, mciId, vmId);

        //        log.info(">>> start checking monitoring agent status");
        //        AgentServiceStatus monitoringStatus =
        //                agentFacadeService.getAgentServiceStatus(nsId, mciId, vmId,
        // Agent.TELEGRAF);
        //        log.info(">>> start checking log agent status");
        //        AgentServiceStatus logStatus =
        //                agentFacadeService.getAgentServiceStatus(nsId, mciId, vmId,
        // Agent.FLUENT_BIT);

        //        savedVM.setMonitoringServiceStatus(monitoringStatus);
        //        savedVM.setLogServiceStatus(logStatus);

        return savedVM;
    }

    public VMDTO getVM(String nsId, String mciId, String vmId) {
        log.info(">>> getVM() called with nsId: {}, mciId: {}, vmId: {}", nsId, mciId, vmId);

        TumblebugMCI.Vm vm;
        String userName;

        VMDTO savedVM = vmService.get(nsId, mciId, vmId);

        try {
            vm = tumblebugService.getVm(nsId, mciId, vmId);
            userName = vm.getVmUserName();
            log.info(
                    ">>> VM fetched: id={}, name={} userName={}",
                    vm.getId(),
                    vm.getName(),
                    userName);
        } catch (Exception e) {
            log.error(">>> getVm() failed", e);
            throw e;
        }

        log.info(">>> start checking monitoring agent status");
        AgentServiceStatus monitoringStatus =
                agentFacadeService.getAgentServiceStatus(nsId, mciId, vmId, Agent.TELEGRAF);
        log.info(">>> start checking log agent status");
        AgentServiceStatus logStatus =
                agentFacadeService.getAgentServiceStatus(nsId, mciId, vmId, Agent.FLUENT_BIT);

        return VMDTO.builder()
                .vmId(vm.getId())
                .name(savedVM.getName())
                .description(vm.getDescription())
                .nsId(nsId)
                .mciId(mciId)
                .monitoringServiceStatus(monitoringStatus)
                .logServiceStatus(logStatus)
                .build();
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
                                            baseDto.getMciId(),
                                            baseDto.getVmId());
                                } catch (Exception e) {
                                    log.error(
                                            ">>> getVM() failed for: nsId={}, mciId={}, vmId={}",
                                            baseDto.getNsId(),
                                            baseDto.getMciId(),
                                            baseDto.getVmId(),
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

    public List<VMDTO> getVMsNsMci(String nsId, String mciId) {

        List<VMDTO> rawList = vmService.getByNsMci(nsId, mciId);

        return fetchVM(rawList);
    }

    public List<VMDTO> getVMs() {
        List<VMDTO> rawList = vmService.list();
        return fetchVM(rawList);
    }

    public VMDTO putVM(String nsId, String mciId, String vmId, VMRequestDTO dto) {
        return vmService.put(nsId, mciId, vmId, dto);
    }

    public void deleteVM(String nsId, String mciId, String vmId) {
        vmService.delete(nsId, mciId, vmId);
    }
}
