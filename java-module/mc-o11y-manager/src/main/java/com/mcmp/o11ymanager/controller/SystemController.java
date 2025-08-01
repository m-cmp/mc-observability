package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.plugin.PluginDefDTO;
import com.mcmp.o11ymanager.global.target.ResBody;
import com.mcmp.o11ymanager.service.AgentPluginDefServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring")
public class SystemController {
    private final AgentPluginDefServiceImpl agentPluginDefServiceImpl;

    @GetMapping("/plugins")
    public ResBody<List<PluginDefDTO>> getPlugins() {
        List<PluginDefDTO> plugins = agentPluginDefServiceImpl.getAllPluginDefinitions()
                .stream()
                .map(entity -> PluginDefDTO.builder()
                        .seq(entity.getSeq())
                        .name(entity.getName())
                        .pluginId(entity.getPluginId())
                        .build())
                .toList();
        return new ResBody<>(plugins);
    }
}
