package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.manager.exception.agent.LogAgentNotInstalled;
import com.mcmp.o11ymanager.manager.exception.agent.MonitoringAgentNotInstalled;
import com.mcmp.o11ymanager.manager.exception.vm.VMAgentTaskProcessingException;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.global.error.ResourceNotExistsException;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.host.VMStatus;
import com.mcmp.o11ymanager.manager.repository.VMJpaRepository;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VMServiceImpl implements VMService {

    private final VMJpaRepository vmJpaRepository;
    private final RequestInfo requestInfo;
    private final ApplicationEventPublisher event;

    @Override
    public VMDTO get(String nsId, String mciId, String vmId) {
        VMEntity entity =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(),
                                                "VMEntity",
                                                nsId + "/" + mciId));
        return VMDTO.fromEntity(entity);
    }

    @Override
    public List<VMDTO> getByNsMci(String nsId, String mciId) {
        return vmJpaRepository.findByNsIdAndMciId(nsId, mciId).stream()
                .map(VMDTO::fromEntity)
                .toList();
    }

    @Override
    public VMDTO getByNsVm(String nsId, String vmId) {
        VMEntity e =
                vmJpaRepository
                        .findByNsIdAndVmId(nsId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(),
                                                "VMEntity",
                                                nsId + "/" + vmId));
        return VMDTO.fromEntity(e);
    }

    @Override
    public List<VMDTO> list() {
        return vmJpaRepository.findAll().stream().map(VMDTO::fromEntity).toList();
    }

    @Override
    public VMDTO post(
            String nsId,
            String mciId,
            String vmId,
            VMStatus vmStatus,
            VMRequestDTO dto,
            Long influxSeq) {
        VMEntity vm =
                VMEntity.builder()
                        .nsId(nsId)
                        .mciId(mciId)
                        .vmId(vmId)
                        .name(dto.getName())
                        .description(dto.getDescription())
                        .nsId(nsId)
                        .mciId(mciId)
                        .vmStatus(vmStatus)
                        .influxSeq(influxSeq)
                        .build();

        VMEntity savedVM = vmJpaRepository.save(vm);

        return VMDTO.fromEntity(savedVM);
    }

    @Override
    public VMDTO put(String nsId, String mciId, String vmId, VMRequestDTO dto) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        if (dto.getName() != null) vm.setName(dto.getName());
        if (dto.getDescription() != null) vm.setDescription(dto.getDescription());

        VMEntity updated = vmJpaRepository.save(vm);

        return VMDTO.fromEntity(updated);
    }

    @Override
    @Transactional
    public void delete(String nsId, String mciId, String vmId) {
        VMEntity entity =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        vmJpaRepository.delete(entity);
    }

    @Override
    public void isIdleMonitoringAgent(String nsId, String mciId, String vmId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        if (vm.getMonitoringAgentTaskStatus() != VMAgentTaskStatus.IDLE) {
            throw new VMAgentTaskProcessingException(
                    requestInfo.getRequestId(),
                    vmId,
                    "monitoringAgentTask",
                    vm.getMonitoringAgentTaskStatus());
        }
    }

    @Override
    public void isIdleLogAgent(String nsId, String mciId, String vmId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        if (vm.getLogAgentTaskStatus() != VMAgentTaskStatus.IDLE) {
            throw new VMAgentTaskProcessingException(
                    requestInfo.getRequestId(), vmId, "로그", vm.getLogAgentTaskStatus());
        }
    }

    @Override
    public void isLogAgentInstalled(String nsId, String mciId, String vmId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        if (vm.getLogServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
            throw new LogAgentNotInstalled(requestInfo.getRequestId(), vmId);
        }
    }

    @Override
    public void isMonitoringAgentInstalled(String nsId, String mciId, String vmId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        if (vm.getMonitoringServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
            throw new MonitoringAgentNotInstalled(requestInfo.getRequestId(), vmId);
        }
    }

    @Override
    public void updateMonitoringAgentTaskStatus(
            String nsId, String mciId, String vmId, VMAgentTaskStatus status) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        vm.setMonitoringAgentTaskStatus(status);

        // TODO 다른 방법 고민할 것
        if (status.equals(VMAgentTaskStatus.IDLE)) {
            vm.setVmMonitoringAgentTaskId("");
        }

        vmJpaRepository.save(vm);
        log.info(
                "[VMService] Monitoring Service Status {}: {}",
                vm.getVmMonitoringAgentTaskId(),
                vm.getMonitoringAgentTaskStatus());
    }

    @Override
    public void updateLogAgentTaskStatus(
            String nsId, String mciId, String vmId, VMAgentTaskStatus status) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        vm.setLogAgentTaskStatus(status);

        // TODO 다른 방법 고민할 것
        if (status.equals(VMAgentTaskStatus.IDLE)) {
            vm.setVmLogAgentTaskId("");
        }

        vmJpaRepository.save(vm);
        log.info(
                "[VMService] Log Service Status {}: {}",
                vm.getVmLogAgentTaskId(),
                vm.getLogServiceStatus());
    }

    @Override
    public void updateMonitoringAgentTaskStatusAndTaskId(
            String nsId, String mciId, String vmId, VMAgentTaskStatus status, String taskId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        vm.setMonitoringAgentTaskStatus(status);
        vm.setVmMonitoringAgentTaskId(taskId);

        vmJpaRepository.save(vm);
        log.info(
                "==================================Monitoring Service Status {}===============================================",
                vm);
    }

    @Override
    public void updateLogAgentTaskStatusAndTaskId(
            String nsId, String mciId, String vmId, VMAgentTaskStatus status, String taskId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndMciIdAndVmId(nsId, mciId, vmId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", vmId));

        vm.setLogAgentTaskStatus(status);
        vm.setVmLogAgentTaskId(taskId);

        vmJpaRepository.save(vm);

        log.info("[VMService] Log Service Status {}", vm);
    }

    @Override
    public List<String> getVmIds(String nsId, String mciId) {
        return vmJpaRepository.findByNsIdAndMciId(nsId, mciId).stream()
                .map(VMEntity::getVmId)
                .toList();
    }

    @Override
    public Long getInfluxId(String nsId, String mciId) {
        VMEntity t =
                vmJpaRepository
                        .findTop1ByNsIdAndMciIdOrderByVmIdAsc(nsId, mciId)
                        .orElseThrow(() -> new IllegalStateException("no vms under ns/mci"));

        return t.getInfluxSeq();
    }
}
