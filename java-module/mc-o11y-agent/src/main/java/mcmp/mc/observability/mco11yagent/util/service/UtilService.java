package mcmp.mc.observability.mco11yagent.util.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilService {
    private final RestTemplate restTemplate;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${feign.cb-tumblebug.url}")
    private String tumblebugURL;

    @Value("${feign.cb-tumblebug.id}")
    private String tumblebugID;

    @Value("${feign.cb-tumblebug.pw}")
    private String tumblebugPW;

    @Value("${feign.cb-spider.url}")
    private String spiderURL;

    @Value("${feign.cb-spider.id}")
    private String spiderID;

    @Value("${feign.cb-spider.pw}")
    private String spiderPW;

    private boolean checkDatabaseConnection() {
        Connection connection = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");

            log.info("Connecting to database with URL: {}", dbUrl);
            connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);

            return connection.isValid(5);
        } catch (SQLException | ClassNotFoundException e) {
            log.error("Database connection error", e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error("Error closing database connection", e);
                }
            }
        }
    }

    public Map<String, Object> checkExternalServiceHealth(String externalServiceUrl, String username, String password) {
        Map<String, Object> externalServiceStatus = new HashMap<>();
        ResponseEntity<String> response;
        boolean isHealthy;

        try {
            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

                HttpEntity<String> entity = new HttpEntity<>(headers);
                response = restTemplate.exchange(externalServiceUrl, HttpMethod.GET, entity, String.class);
            } else {
                response = restTemplate.exchange(externalServiceUrl, HttpMethod.GET, null, String.class);
            }

            isHealthy = (response.getStatusCodeValue() == 200);
        } catch (Exception e) {
            isHealthy = false;
            log.error("Error checking health for external service: " + externalServiceUrl, e);
        }

        externalServiceStatus.put("status", isHealthy ? "UP" : "DOWN");

        return externalServiceStatus;
    }

    public Map<String, Object> checkApiHealth() {
        Map<String, Object> apiHealthStatus = new HashMap<>();

        Map<String, Object> spiderStatus = checkExternalServiceHealth(spiderURL + "/spider/readyz", spiderID, spiderPW);
        Map<String, Object> tumblebugStatus = checkExternalServiceHealth(tumblebugURL + "/tumblebug/readyz", tumblebugID, tumblebugPW);

        boolean isDatabaseHealthy = checkDatabaseConnection();
        boolean isSpiderHealthy = "UP".equals(spiderStatus.get("status"));
        boolean isTumblebugHealthy = "UP".equals(tumblebugStatus.get("status"));

        boolean isApiHealthy = isDatabaseHealthy && isSpiderHealthy && isTumblebugHealthy;

        if (isApiHealthy) {
            apiHealthStatus.put("message", "All systems are operational");
        } else {
            StringBuilder message = new StringBuilder("One or more systems are down: ");
            if (!isDatabaseHealthy) {
                message.append("Database is down. ");
            }
            if (!isSpiderHealthy) {
                message.append("Spider is down. ");
            }
            if (!isTumblebugHealthy) {
                message.append("Tumblebug is down. ");
            }
            apiHealthStatus.put("message", message.toString().trim());
        }

        return apiHealthStatus;
    }
}
