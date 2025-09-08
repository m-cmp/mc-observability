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

    @MessageMapping("/vm/{nsId}/{mciId]/{vmId}")
    @SendTo("/topic/vm/{nsId}/{mciId]/{vmId}")
    public VMDTO subscribeVMInfo(
            @DestinationVariable String nsId,
            @DestinationVariable String mciId,
            @DestinationVariable String vmId) {
        return vmFacadeService.getVM(nsId, mciId, vmId);
    }

    @MessageMapping("/vms")
    @SendTo("/topic/vms")
    public Object subscribeAllVMs() {
        return vmFacadeService.getVMs();
    }
}
