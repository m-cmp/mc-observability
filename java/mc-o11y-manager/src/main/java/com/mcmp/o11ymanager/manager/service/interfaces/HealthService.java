package com.mcmp.o11ymanager.manager.service.interfaces;

import java.util.Map;

public interface HealthService {

    public Map<String, Object> checkExternalServiceHealth(String externalServiceUrl);

    public Map<String, Object> checkExternalServiceHealth(
            String externalServiceUrl, String username, String password);

    public Map<String, Object> checkApiHealth();
}
