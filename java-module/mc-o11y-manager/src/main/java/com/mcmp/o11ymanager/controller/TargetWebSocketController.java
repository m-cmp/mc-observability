package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.target.TargetDTO;

import com.mcmp.o11ymanager.facade.TargetFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TargetWebSocketController {

    private final TargetFacadeService targetFacadeService;

    @MessageMapping("/target/{nsId}/{mciId]/{targetId}")
    @SendTo("/topic/target/{nsId}/{mciId]/{targetId}")
    public TargetDTO subscribeTargetInfo(
            @DestinationVariable String nsId, @DestinationVariable String mciId, @DestinationVariable String targetId) {
        return targetFacadeService.getTarget(nsId, mciId, targetId);
    }

    @MessageMapping("/targets")
    @SendTo("/topic/targets")
    public Object subscribeAllTargets() {
        return targetFacadeService.getTargets();
    }
}
