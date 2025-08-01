package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.entity.AgentPluginDefEntity;
import java.util.List;
import org.springframework.core.io.Resource;

public interface AgentPluginDefService {

  List<AgentPluginDefEntity> getAllPluginDefinitions();


  void initializePluginDefinitions();

  List<AgentPluginDefEntity> parsePluginsFromResources();

  String extractPluginNameFromFilename(String filename);

  String parsePluginIdFromFile(Resource resource, String pluginName);

}
