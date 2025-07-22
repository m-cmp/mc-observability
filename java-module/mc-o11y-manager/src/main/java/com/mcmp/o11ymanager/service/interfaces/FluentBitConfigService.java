package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.dto.host.ConfigResponseDTO;
import org.springframework.core.io.ClassPathResource;

public interface FluentBitConfigService {
    ConfigResponseDTO getFluentBitConfigTemplate(String path);

    ClassPathResource getFluentBitResource(String path);
}
