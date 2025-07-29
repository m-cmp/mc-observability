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
    
    private static final Pattern INPUT_PATTERN = Pattern.compile("\\[\\[inputs\\.(\\w+)\\]\\]");
    private static final Pattern OUTPUT_PATTERN = Pattern.compile("\\[\\[outputs\\.(\\w+)\\]\\]");

    public List<AgentPluginDefEntity> getAllPluginDefinitions() {
        return agentPluginDefRepository.findAll();
    }

    @Transactional
    public void initializePluginDefinitions() {
        log.info("Starting to initialize agent plugin definitions from telegraf config files");
        
        List<AgentPluginDefEntity> pluginDefs = new ArrayList<>();
        
        // Parse input plugins
        pluginDefs.addAll(parsePluginsFromResources("telegraf_inputs_*", "INPUT"));
        
        // Parse output plugins  
        pluginDefs.addAll(parsePluginsFromResources("telegraf_outputs_*", "OUTPUT"));
        
        // Clear existing data and save new definitions
        agentPluginDefRepository.deleteAll();
        agentPluginDefRepository.saveAll(pluginDefs);
        
        log.info("Successfully initialized {} plugin definitions", pluginDefs.size());
    }

    private List<AgentPluginDefEntity> parsePluginsFromResources(String pattern, String pluginType) {
        List<AgentPluginDefEntity> pluginDefs = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        
        try {
            Resource[] resources = resolver.getResources("classpath*:" + pattern);
            log.info("Found {} resources for pattern: classpath*:{}", resources.length, pattern);


            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null) {
                    String pluginName = extractPluginNameFromFilename(filename);
                    String pluginId = parsePluginIdFromFile(resource, pluginType, pluginName);
                    
                    if (pluginName != null && pluginId != null) {
                        AgentPluginDefEntity entity = AgentPluginDefEntity.builder()
                                .name(pluginName)
                                .pluginId(pluginId)
                                .pluginType(pluginType)
                                .build();
                        pluginDefs.add(entity);
                        log.debug("Found {} plugin: {} -> {}", pluginType, pluginName, pluginId);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading telegraf config files with pattern: {}", pattern, e);
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

    private String parsePluginIdFromFile(Resource resource, String pluginType, String pluginName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                
                if ("INPUT".equals(pluginType)) {
                    Matcher matcher = INPUT_PATTERN.matcher(trimmedLine);
                    if (matcher.find()) {
                        return "[[inputs." + pluginName + "]]";
                    }
                } else if ("OUTPUT".equals(pluginType)) {
                    Matcher matcher = OUTPUT_PATTERN.matcher(trimmedLine);
                    if (matcher.find()) {
                        return "[[outputs." + pluginName + "]]";
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading file: {}", resource.getFilename(), e);
        }
        
        return null;
    }
}