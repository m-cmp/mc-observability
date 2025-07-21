package com.mcmp.o11ymanager.oldService.domain;

import com.mcmp.o11ymanager.dto.host.HostResponseDTO;
import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.mapper.host.HostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HostNotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final HostMapper hostMapper;

    public void notifyAllHostsUpdate(List<HostEntity> hostList) {
        List<HostResponseDTO> hostResponseList = new ArrayList<>();

        for (HostEntity host: hostList) {
            hostResponseList.add(hostMapper.toDTO(host));
        }

        messagingTemplate.convertAndSend("/topic/hosts", hostResponseList);
    }

    public void notifyHostUpdate(String hostId, HostResponseDTO hostInfo) {
        messagingTemplate.convertAndSend("/topic/host/" + hostId, hostInfo);
    }
}
