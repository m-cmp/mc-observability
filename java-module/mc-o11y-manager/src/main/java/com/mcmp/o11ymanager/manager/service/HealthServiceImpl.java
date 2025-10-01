package com.mcmp.o11ymanager.manager.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthServiceImpl {

    @Value("${feign.cb-tumblebug.url}")
    private String tumblebugURL;

    @Value("${feign.cb-tumblebug.id}")
    private String tumblebugID;

    @Value("${feign.cb-tumblebug.pw}")
    private String tumblebugPW;

    @Value("${feign.insight.url}")
    private String insightURL;

    public Map<String, Object> checkExternalServiceHealth(String externalServiceUrl) {
        return checkExternalServiceHealth(externalServiceUrl, null, null);
    }

    public Map<String, Object> checkExternalServiceHealth(
            String externalServiceUrl, String username, String password) {
        Map<String, Object> externalServiceStatus = new HashMap<>();
        ResponseEntity<String> response;
        boolean isHealthy;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        RestTemplate restTemplate = new RestTemplate(factory);

        try {
            if (username != null
                    && !username.isEmpty()
                    && password != null
                    && !password.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

                HttpEntity<String> entity = new HttpEntity<>(headers);
                response =
                        restTemplate.exchange(
                                externalServiceUrl, HttpMethod.GET, entity, String.class);
            } else {
                response =
                        restTemplate.exchange(
                                externalServiceUrl, HttpMethod.GET, null, String.class);
            }

            isHealthy = (response.getStatusCode().value() == 200);
        } catch (Exception e) {
            isHealthy = false;
            log.error("Error checking health for external service: " + externalServiceUrl, e);
        }

        externalServiceStatus.put("status", isHealthy ? "UP" : "DOWN");

        return externalServiceStatus;
    }

    public Map<String, Object> checkApiHealth() {
        Map<String, Object> apiHealthStatus = new HashMap<>();

        Map<String, Object> tumblebugStatus =
                checkExternalServiceHealth(
                        tumblebugURL + "/tumblebug/readyz", tumblebugID, tumblebugPW);
        Map<String, Object> insightStatus =
                checkExternalServiceHealth(insightURL + "/api/o11y/insight/predictions/options");

        boolean isTumblebugHealthy = "UP".equals(tumblebugStatus.get("status"));
        boolean isInsightHealthy = "UP".equals(insightStatus.get("status"));

        boolean isApiHealthy = isTumblebugHealthy && isInsightHealthy;

        if (isApiHealthy) {
            apiHealthStatus.put("message", "All systems are operational");
        } else {
            StringBuilder message = new StringBuilder("One or more systems are down: ");
            if (!isTumblebugHealthy) {
                message.append("Tumblebug is down. ");
            }
            if (!isInsightHealthy) {
                message.append("Insight is down. ");
            }
            apiHealthStatus.put("message", message.toString().trim());
        }

        return apiHealthStatus;
    }
}
