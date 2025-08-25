package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.dto.plugin.PluginDefDTO;
import com.mcmp.o11ymanager.entity.AgentPluginDefEntity;
import com.mcmp.o11ymanager.repository.AgentPluginDefJpaRepository;
import com.mcmp.o11ymanager.service.interfaces.AgentPluginDefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPluginDefServiceImpl implements AgentPluginDefService {

    public final AgentPluginDefJpaRepository agentPluginDefRepository;
    
    public static final Pattern INPUT_PATTERN = Pattern.compile("\\[\\[inputs\\.(\\w+)]]");


    public List<PluginDefDTO> getAllPluginDefinitions() {
        return agentPluginDefRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }


    public String getPluginType(String pluginId) {
        if (pluginId == null)
            return "UNKNOWN";

        Matcher matcher_input = INPUT_PATTERN.matcher(pluginId);
        if (matcher_input.matches()) {
            return "INPUT";
        }

        return "UNKNOWN";
    }


    private PluginDefDTO toDto(AgentPluginDefEntity entity) {
        return PluginDefDTO.builder()
            .seq(entity.getSeq())
            .name(entity.getName())
            .pluginId(entity.getPluginId())
            .pluginType(getPluginType(entity.getPluginId()))
            .build();
    }

    @Override
    @Transactional
    public void initializePluginDefinitions() {
        log.info("Starting to initialize agent plugin definitions from telegraf config files");

        // Parse input plugins from resources
        List<AgentPluginDefEntity> newPluginDefs = parsePluginsFromResources();

        // Get existing definitions
        List<AgentPluginDefEntity> existingDefs = agentPluginDefRepository.findAll();

        // Create maps for easy comparison
        Map<String, AgentPluginDefEntity> existingDefsMap = existingDefs.stream()
            .collect(Collectors.toMap(
                entity -> entity.getName() + "_" + entity.getPluginId(),
                entity -> entity
            ));

        Map<String, AgentPluginDefEntity> newDefsMap = newPluginDefs.stream()
            .collect(Collectors.toMap(
                entity -> entity.getName() + "_" + entity.getPluginId(),
                entity -> entity
            ));

        // Find definitions to add (exist in new but not in existing)
        List<AgentPluginDefEntity> toAdd = new ArrayList<>();
        for (AgentPluginDefEntity newDef : newPluginDefs) {
            String key = newDef.getName() + "_" + newDef.getPluginId();
            if (!existingDefsMap.containsKey(key)) {
                toAdd.add(newDef);
            }
        }

        // Find definitions to remove (exist in existing but not in new)
        List<AgentPluginDefEntity> toRemove = new ArrayList<>();
        for (AgentPluginDefEntity existingDef : existingDefs) {
            String key = existingDef.getName() + "_" + existingDef.getPluginId();
            if (!newDefsMap.containsKey(key)) {
                toRemove.add(existingDef);
            }
        }

        // Apply changes
        if (!toRemove.isEmpty()) {
            agentPluginDefRepository.deleteAll(toRemove);
            log.info("Removed {} obsolete plugin definitions", toRemove.size());
        }

        if (!toAdd.isEmpty()) {
            agentPluginDefRepository.saveAll(toAdd);
            log.info("Added {} new plugin definitions", toAdd.size());
        }

        log.info("Successfully synchronized plugin definitions. Total: {}",
            existingDefs.size() - toRemove.size() + toAdd.size());
    }

    @Override
    public List<AgentPluginDefEntity> parsePluginsFromResources() {
        List<AgentPluginDefEntity> pluginDefs = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources("classpath*:" + "telegraf_inputs_*");
            log.info("Found {} resources for pattern: classpath*:{}", resources.length, "telegraf_inputs_*");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null) {
                    String pluginName = extractPluginNameFromFilename(filename);
                    String pluginId = parsePluginIdFromFile(resource, pluginName);

                    if (pluginName != null && pluginId != null) {
                        AgentPluginDefEntity entity = AgentPluginDefEntity.builder()
                                .name(pluginName)
                                .pluginId(pluginId)
                                .build();
                        pluginDefs.add(entity);
                        log.debug("Found {} plugin: {} -> {}", "INPUT", pluginName, pluginId);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading telegraf config files with pattern: {}", "telegraf_inputs_*", e);
        }

        return pluginDefs;
    }

    @Override
    public String extractPluginNameFromFilename(String filename) {
        if (filename.startsWith("telegraf_inputs_")) {
            return filename.substring("telegraf_inputs_".length());
        } else if (filename.startsWith("telegraf_outputs_")) {
            return filename.substring("telegraf_outputs_".length());
        }
        return null;
    }

    @Override
    public String parsePluginIdFromFile(Resource resource, String pluginName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                Matcher matcher = INPUT_PATTERN.matcher(trimmedLine);
                if (matcher.find()) {
                    return "[[inputs." + pluginName + "]]";
                }
            }
        } catch (IOException e) {
            log.error("Error reading file: {}", resource.getFilename(), e);
        }

        return null;
    }
}
