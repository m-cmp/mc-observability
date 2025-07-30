package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.entity.AgentPluginDefEntity;
import com.mcmp.o11ymanager.repository.AgentPluginDefJpaRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPluginDefService {

    private final AgentPluginDefJpaRepository agentPluginDefRepository;
    
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\[\\[inputs\\.(\\w+)]]");

    public List<AgentPluginDefEntity> getAllPluginDefinitions() {
        return agentPluginDefRepository.findAll();
    }

    @Transactional
    public void initializePluginDefinitions() {
        log.info("Starting to initialize agent plugin definitions from telegraf config files");

        // Parse input plugins
        List<AgentPluginDefEntity> pluginDefs = new ArrayList<>(parsePluginsFromResources());
        
        // Clear existing data and save new definitions
        agentPluginDefRepository.deleteAll();
        agentPluginDefRepository.saveAll(pluginDefs);
        
        log.info("Successfully initialized {} plugin definitions", pluginDefs.size());
    }

    private List<AgentPluginDefEntity> parsePluginsFromResources() {
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

    private String extractPluginNameFromFilename(String filename) {
        if (filename.startsWith("telegraf_inputs_")) {
            return filename.substring("telegraf_inputs_".length());
        } else if (filename.startsWith("telegraf_outputs_")) {
            return filename.substring("telegraf_outputs_".length());
        }
        return null;
    }

    private String parsePluginIdFromFile(Resource resource, String pluginName) {
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