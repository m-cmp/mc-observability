package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.exception.host.HostAgentTaskProcessingException;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.host.VMStatus;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public interface VMService {

    ReentrantLock getHostLock(String nsId, String infraId, String nodeId);

    VMDTO get(String nsId, String infraId, String nodeId);

    List<VMDTO> getByNsMci(String nsId, String infraId);

    VMDTO getByNsVm(String nsId, String nodeId);

    List<VMDTO> list();

    VMDTO post(
            String nsId,
            String infraId,
            String nodeId,
            VMStatus vmStatus,
            VMRequestDTO dto,
            Long influxSeq);

    VMDTO put(String nsId, String infraId, String nodeId, VMRequestDTO dto);

    void delete(String nsId, String infraId, String nodeId);

    void isIdleMonitoringAgent(String nsId, String infraId, String nodeId)
            throws HostAgentTaskProcessingException;

    void isIdleLogAgent(String nsId, String infraId, String nodeId)
            throws HostAgentTaskProcessingException;

    void isIdleTraceAgent(String nsId, String infraId, String nodeId)
            throws HostAgentTaskProcessingException;

    VMAgentTaskStatus getMonitoringAgentTaskStatus(String nsId, String infraId, String nodeId);

    VMAgentTaskStatus getLogAgentTaskStatus(String nsId, String infraId, String nodeId);

    VMAgentTaskStatus getTraceAgentTaskStatus(String nsId, String infraId, String nodeId);

    void updateMonitoringAgentTaskStatus(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status);

    void updateLogAgentTaskStatus(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status);

    void updateTraceAgentTaskStatus(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status);

    void updateMonitoringAgentTaskStatusAndTaskId(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status, String taskId);

    void updateLogAgentTaskStatusAndTaskId(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status, String taskId);

    void updateTraceAgentTaskStatusAndTaskId(
            String nsId, String infraId, String nodeId, VMAgentTaskStatus status, String taskId);

    List<String> getNodeIds(String nsId, String infraId);

    Long getInfluxId(String nsId, String infraId);
}
