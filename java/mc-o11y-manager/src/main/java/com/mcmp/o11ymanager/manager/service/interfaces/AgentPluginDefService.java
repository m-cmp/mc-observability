package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.plugin.PluginDefDTO;
import com.mcmp.o11ymanager.manager.entity.AgentPluginDefEntity;
import java.util.List;
import org.springframework.core.io.Resource;

public interface AgentPluginDefService {

    List<PluginDefDTO> getAllPluginDefinitions();

    void initializePluginDefinitions();

    List<AgentPluginDefEntity> parsePluginsFromResources();

    String extractPluginNameFromFilename(String filename);

    String parsePluginIdFromFile(Resource resource, String pluginName);
}
