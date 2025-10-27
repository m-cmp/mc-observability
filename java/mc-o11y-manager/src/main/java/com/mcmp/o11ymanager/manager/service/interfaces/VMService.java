package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.exception.host.HostAgentTaskProcessingException;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.model.host.VMStatus;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public interface VMService {

    ReentrantLock getHostLock(String nsId, String mciId, String vmId);

    VMDTO get(String nsId, String mciId, String vmId);

    List<VMDTO> getByNsMci(String nsId, String mciId);

    VMDTO getByNsVm(String nsId, String vmId);

    List<VMDTO> list();

    VMDTO post(
            String nsId,
            String mciId,
            String vmId,
            VMStatus vmStatus,
            VMRequestDTO dto,
            Long influxSeq);

    VMDTO put(String nsId, String mciId, String vmId, VMRequestDTO dto);

    void delete(String nsId, String mciId, String vmId);

    void isIdleMonitoringAgent(String nsId, String mciId, String vmId)
            throws HostAgentTaskProcessingException;

    void isIdleLogAgent(String nsId, String mciId, String vmId)
            throws HostAgentTaskProcessingException;

    VMAgentTaskStatus getMonitoringAgentTaskStatus(String nsId, String mciId, String vmId);

    VMAgentTaskStatus getLogAgentTaskStatus(String nsId, String mciId, String vmId);

    void updateMonitoringAgentTaskStatus(
            String nsId, String mciId, String vmId, VMAgentTaskStatus status);

    void updateLogAgentTaskStatus(String nsId, String mciId, String vmId, VMAgentTaskStatus status);

    void updateMonitoringAgentTaskStatusAndTaskId(
            String nsId, String mciId, String vmId, VMAgentTaskStatus status, String taskId);

    void updateLogAgentTaskStatusAndTaskId(
            String nsId, String mciId, String vmId, VMAgentTaskStatus status, String taskId);

    List<String> getVmIds(String nsId, String mciId);

    Long getInfluxId(String nsId, String mciId);
}
