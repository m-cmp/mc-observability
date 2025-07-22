package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.dto.host.ConfigResponseDTO;

public interface TelegrafConfigService {

    String generateTelegrafConfig(String uuid, String hostType, String metrics);

    ConfigResponseDTO getTelegrafConfigTemplate(String path);

}
