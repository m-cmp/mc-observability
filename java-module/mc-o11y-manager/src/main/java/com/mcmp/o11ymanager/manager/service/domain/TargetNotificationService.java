package com.mcmp.o11ymanager.manager.service.domain;

import com.mcmp.o11ymanager.manager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.manager.entity.TargetEntity;
import com.mcmp.o11ymanager.manager.mapper.host.TargetMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TargetNotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final TargetMapper targetMapper;

    public void notifyAllTargetsUpdate(List<TargetEntity> targetList) {
        List<TargetDTO> hostResponseList = new ArrayList<>();

        for (TargetEntity target : targetList) {
            hostResponseList.add(targetMapper.toDTO(target));
        }

        messagingTemplate.convertAndSend("/topic/targets", hostResponseList);
    }

    public void notifyTargetUpdate(
            String nsId, String mciId, String targetId, TargetDTO targetInfo) {
        messagingTemplate.convertAndSend(
                "/topic/target/" + nsId + "/" + mciId + "/" + targetId, targetInfo);
    }
}
