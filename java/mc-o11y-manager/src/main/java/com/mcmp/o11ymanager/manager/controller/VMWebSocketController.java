package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.facade.VMFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VMWebSocketController {

    private final VMFacadeService vmFacadeService;

    @MessageMapping("/node/{nsId}/{infraId}/{nodeId}")
    @SendTo("/topic/node/{nsId}/{infraId}/{nodeId}")
    public VMDTO subscribeVMInfo(
            @DestinationVariable String nsId,
            @DestinationVariable String infraId,
            @DestinationVariable String nodeId) {
        return vmFacadeService.getVM(nsId, infraId, nodeId);
    }

    @MessageMapping("/nodes")
    @SendTo("/topic/nodes")
    public Object subscribeAllVMs() {
        return vmFacadeService.getVMs();
    }
}
