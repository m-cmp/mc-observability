package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.exception.vm.VMAgentTaskProcessingException;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.manager.global.error.ResourceNotExistsException;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.host.VMStatus;
import com.mcmp.o11ymanager.manager.repository.VMJpaRepository;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VMServiceImpl implements VMService {

    private final VMJpaRepository vmJpaRepository;
    private final RequestInfo requestInfo;

    private final ConcurrentHashMap<String, ReentrantLock> repositoryLocks =
            new ConcurrentHashMap<>();

    public ReentrantLock getHostLock(String nsId, String infraId, String nodeId) {
        String lockKey = nsId + "-" + infraId + "-" + nodeId;
        return repositoryLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
    }

    @Override
    public VMDTO get(String nsId, String infraId, String nodeId) {
        VMEntity entity =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(),
                                                "VMEntity",
                                                nsId + "/" + infraId));
        return VMDTO.fromEntity(entity);
    }

    @Override
    public List<VMDTO> getByNsMci(String nsId, String infraId) {
        return vmJpaRepository.findByNsIdAndInfraId(nsId, infraId).stream()
                .map(VMDTO::fromEntity)
                .toList();
    }

    @Override
    public VMDTO getByNsVm(String nsId, String nodeId) {
        VMEntity e =
                vmJpaRepository
                        .findByNsIdAndNodeId(nsId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(),
                                                "VMEntity",
                                                nsId + "/" + nodeId));
        return VMDTO.fromEntity(e);
    }

    @Override
    public List<VMDTO> list() {
        return vmJpaRepository.findAll().stream().map(VMDTO::fromEntity).toList();
    }

    @Override
    public VMDTO post(
            String nsId,
            String infraId,
            String nodeId,
            VMStatus vmStatus,
            VMRequestDTO dto,
            Long influxSeq) {
        VMEntity vm =
                VMEntity.builder()
                        .nsId(nsId)
                        .infraId(infraId)
                        .nodeId(nodeId)
                        .name(dto.getName())
                        .description(dto.getDescription())
                        .nsId(nsId)
                        .infraId(infraId)
                        .vmStatus(vmStatus)
                        .influxSeq(influxSeq)
                        .build();

        VMEntity savedVM = vmJpaRepository.save(vm);

        return VMDTO.fromEntity(savedVM);
    }

    @Override
    public VMDTO put(String nsId, String infraId, String nodeId, VMRequestDTO dto) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        if (dto.getName() != null) vm.setName(dto.getName());
        if (dto.getDescription() != null) vm.setDescription(dto.getDescription());

        VMEntity updated = vmJpaRepository.save(vm);

        return VMDTO.fromEntity(updated);
    }

    @Override
    @Transactional
    public void delete(String nsId, String infraId, String nodeId) {
        VMEntity entity =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        vmJpaRepository.delete(entity);
    }

    @Override
    public VMAgentTaskStatus getMonitoringAgentTaskStatus(
            String nsId, String infraId, String nodeId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));
        return vm.getMonitoringAgentTaskStatus();
    }

    @Override
    public VMAgentTaskStatus getLogAgentTaskStatus(String nsId, String infraId, String nodeId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));
        return vm.getLogAgentTaskStatus();
    }

    @Override
    public VMAgentTaskStatus getTraceAgentTaskStatus(String nsId, String infraId, String nodeId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));
        return vm.getTraceAgentTaskStatus();
    }

    @Override
    public void isIdleMonitoringAgent(String nsId, String infraId, String nodeId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        if (vm.getMonitoringAgentTaskStatus() != VMAgentTaskStatus.IDLE) {
            throw new VMAgentTaskProcessingException(
                    requestInfo.getRequestId(),
                    nodeId,
                    "monitoringAgentTask",
                    vm.getMonitoringAgentTaskStatus());
        }
    }

    @Override
    public void isIdleLogAgent(String nsId, String infraId, String nodeId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        if (vm.getLogAgentTaskStatus() != VMAgentTaskStatus.IDLE) {
            throw new VMAgentTaskProcessingException(
                    requestInfo.getRequestId(), nodeId, "로그", vm.getLogAgentTaskStatus());
        }
    }

    @Override
    public void isIdleTraceAgent(String nsId, String infraId, String nodeId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        VMAgentTaskStatus status = vm.getTraceAgentTaskStatus();
        // NULL은 아직 Beyla 작업이 한 번도 없었던 상태로 간주하고 IDLE과 동일하게 취급.
        // ddl-auto:update로 trace_agent_task_status 컬럼이 새로 추가되면서
        // 기존 VM 레코드들이 NULL로 남아있는 경우를 대응.
        if (status != null && status != VMAgentTaskStatus.IDLE) {
            throw new VMAgentTaskProcessingException(
                    requestInfo.getRequestId(), nodeId, "traceAgent", status);
        }
    }

    @Override
    public void updateMonitoringAgentTaskStatus(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        vm.setMonitoringAgentTaskStatus(status);

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
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        vm.setLogAgentTaskStatus(status);

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
    public void updateTraceAgentTaskStatus(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status) {
        // 특정 VM의 Beyla 작업 상태를 DB에 업데이트
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(
                                nsId, infraId, nodeId) // nsId + infraId + nodeId 복합키로 VMEntity 조회
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        vm.setTraceAgentTaskStatus(status); // traceAgentTaskStatus 필드에 새 상태 세팅

        if (status.equals(VMAgentTaskStatus.IDLE)) {
            // 상태가 IDEL 이면  vmTraceAgentTaskId를 빈문자로 초기화 (작업 종료를 의미)
            vm.setVmTraceAgentTaskId("");
        }

        vmJpaRepository.save(vm);
        log.info(
                "[VMService] Trace Agent Status {}: {}",
                vm.getVmTraceAgentTaskId(),
                vm.getTraceAgentTaskStatus());
    }

    @Override
    public void updateMonitoringAgentTaskStatusAndTaskId(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status, String taskId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        vm.setMonitoringAgentTaskStatus(status);
        vm.setVmMonitoringAgentTaskId(taskId);

        vmJpaRepository.save(vm);
        log.info(
                "==================================Monitoring Service Status {}===============================================",
                vm);
    }

    @Override
    public void updateLogAgentTaskStatusAndTaskId(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status, String taskId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        vm.setLogAgentTaskStatus(status);
        vm.setVmLogAgentTaskId(taskId);

        vmJpaRepository.save(vm);

        log.info("[VMService] Log Service Status {}", vm);
    }

    @Override
    public void updateTraceAgentTaskStatusAndTaskId(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status, String taskId) {
        VMEntity vm =
                vmJpaRepository
                        .findByNsIdAndInfraIdAndNodeId(nsId, infraId, nodeId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotExistsException(
                                                requestInfo.getRequestId(), "VMEntity", nodeId));

        vm.setTraceAgentTaskStatus(status);
        vm.setVmTraceAgentTaskId(taskId);

        vmJpaRepository.save(vm);

        log.info("[VMService] Trace Agent Status {}", vm);
    }

    @Override
    public List<String> getNodeIds(String nsId, String infraId) {
        return vmJpaRepository.findByNsIdAndInfraId(nsId, infraId).stream()
                .map(VMEntity::getNodeId)
                .toList();
    }

    @Override
    public Long getInfluxId(String nsId, String infraId) {
        VMEntity t =
                vmJpaRepository
                        .findTop1ByNsIdAndInfraIdOrderByNodeIdAsc(nsId, infraId)
                        .orElseThrow(() -> new IllegalStateException("no vms under ns/mci"));

        return t.getInfluxSeq();
    }

    @Override
    @Transactional
    public void resetAllHostAgentTaskStatus() {
        List<VMEntity> all = vmJpaRepository.findAll();
        int reset = 0;
        for (VMEntity vm : all) {
            boolean changed = false;
            if (isInProgress(vm.getMonitoringAgentTaskStatus())) {
                vm.setMonitoringAgentTaskStatus(VMAgentTaskStatus.IDLE);
                vm.setVmMonitoringAgentTaskId("");
                changed = true;
            }
            if (isInProgress(vm.getLogAgentTaskStatus())) {
                vm.setLogAgentTaskStatus(VMAgentTaskStatus.IDLE);
                vm.setVmLogAgentTaskId("");
                changed = true;
            }
            if (isInProgress(vm.getTraceAgentTaskStatus())) {
                vm.setTraceAgentTaskStatus(VMAgentTaskStatus.IDLE);
                vm.setVmTraceAgentTaskId("");
                changed = true;
            }
            if (changed) {
                vmJpaRepository.save(vm);
                reset++;
                log.info(
                        "[AGENT-TASK-RESET] reset stuck task status ns={} infra={} node={}",
                        vm.getNsId(),
                        vm.getInfraId(),
                        vm.getNodeId());
            }
        }
        log.info("[AGENT-TASK-RESET] reset {} node(s) with in-progress agent task status", reset);
    }

    /** In-progress = anything that is not a terminal/idle state, i.e. a task that was running. */
    private static boolean isInProgress(VMAgentTaskStatus status) {
        return status != null
                && status != VMAgentTaskStatus.IDLE
                && status != VMAgentTaskStatus.FINISHED
                && status != VMAgentTaskStatus.FAILED
                && status != VMAgentTaskStatus.NOT_INSTALLED;
    }
}
