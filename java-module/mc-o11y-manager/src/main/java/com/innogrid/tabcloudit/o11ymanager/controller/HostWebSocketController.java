package com.innogrid.tabcloudit.o11ymanager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.facade.HostFacadeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HostWebSocketController {

    private final HostFacadeService hostFacadeService;

    @MessageMapping("/host/{id}")
    @SendTo("/topic/host/{id}")
    public HostResponseDTO subscribeHostInfo(@DestinationVariable String id) {
        return hostFacadeService.getHost(id);
    }

    @MessageMapping("/hosts")
    @SendTo("/topic/hosts")
    public Object subscribeAllHosts() throws JsonProcessingException {
        List<HostResponseDTO> result = hostFacadeService.list();
        String json = new ObjectMapper().writeValueAsString(result);
        log.info("직렬화된 JSON: {}", json);
        return hostFacadeService.list();
    }
}
