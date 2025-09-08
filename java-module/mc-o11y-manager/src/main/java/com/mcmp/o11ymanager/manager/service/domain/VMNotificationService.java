package com.mcmp.o11ymanager.manager.service.domain;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.mapper.host.VMMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VMNotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final VMMapper vmMapper;

    public void notifyAllVMsUpdate(List<VMEntity> vmList) {
        List<VMDTO> hostResponseList = new ArrayList<>();

        for (VMEntity vm : vmList) {
            hostResponseList.add(vmMapper.toDTO(vm));
        }

        messagingTemplate.convertAndSend("/topic/vms", hostResponseList);
    }

    public void notifyVMUpdate(String nsId, String mciId, String vmId, VMDTO vmInfo) {
        messagingTemplate.convertAndSend("/topic/vm/" + nsId + "/" + mciId + "/" + vmId, vmInfo);
    }
}
