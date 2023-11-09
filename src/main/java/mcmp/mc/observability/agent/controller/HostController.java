package mcmp.mc.observability.agent.controller;

import lombok.RequiredArgsConstructor;
import mcmp.mc.observability.agent.service.HostService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HostController {
    private final HostService hostService;
}
