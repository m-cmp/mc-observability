package com.mcmp.o11ymanager.manager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.mcmp.o11ymanager.manager.dto.plugin.PluginDefDTO;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.service.AgentPluginDefServiceImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring")
@Tag(name = "[Manager] Environment")
public class SystemController {
    private final AgentPluginDefServiceImpl agentPluginDefServiceImpl;

    @GetMapping("/plugins")
    @Operation(summary = "GetPlugins", operationId = "GetPlugins", description = "Retrieve plugin list")
    public ResBody<List<PluginDefDTO>> getPlugins() {
        return new ResBody<>(agentPluginDefServiceImpl.getAllPluginDefinitions());
    }
}
