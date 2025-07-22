package com.mcmp.o11ymanager.oldService.domain;

import com.mcmp.o11ymanager.dto.host.ConfigResponseDTO;
import com.mcmp.o11ymanager.global.annotation.Base64Encode;
import com.mcmp.o11ymanager.global.definition.ConfigDefinition;
import com.mcmp.o11ymanager.oldService.domain.interfaces.FileService;
import com.mcmp.o11ymanager.service.interfaces.FluentBitConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class FluentBitConfigServiceImpl implements FluentBitConfigService {

    private final FileService fileService;

    @Value("${loki.url}")
    private String lokiURL;


    private final ClassPathResource fluentBitMainConfig = new ClassPathResource("fluent-bit.conf");
    private final ClassPathResource fluentBitVariables = new ClassPathResource("fluent-bit_variables");
    private final ClassPathResource fluentBitLogLevelLua = new ClassPathResource("log-level.lua");
    private final ClassPathResource fluentBitParsersConf = new ClassPathResource("parsers.conf");


    @Override
    @Base64Encode
    public ConfigResponseDTO getFluentBitConfigTemplate(String path) {
        if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_MAIN_CONFIG)) {
            return ConfigResponseDTO.builder().content(fileService.getClassResourceContent(fluentBitMainConfig)).build();
        } else if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_PARSERS_CONFIG)) {
            return ConfigResponseDTO.builder().content(fileService.getClassResourceContent(fluentBitParsersConf)).build();
        } else if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_LOG_LEVEL_LUA)) {
            return ConfigResponseDTO.builder().content(fileService.getClassResourceContent(fluentBitLogLevelLua)).build();
        } else if (Objects.equals(path, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES)) {
            return ConfigResponseDTO.builder().content(fileService.getClassResourceContent(fluentBitVariables)).build();
        }

        throw new IllegalArgumentException("Invalid Fluent-Bit template path");
    }

    @Override
    public ClassPathResource getFluentBitResource(String path) {
        return switch (path) {
            case ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_MAIN_CONFIG -> fluentBitMainConfig;
            case ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_PARSERS_CONFIG -> fluentBitParsersConf;
            case ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_LOG_LEVEL_LUA -> fluentBitLogLevelLua;
            case ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES -> fluentBitVariables;
            default -> throw new IllegalArgumentException("Invalid Fluent-Bit resource path");
        };
    }

}
