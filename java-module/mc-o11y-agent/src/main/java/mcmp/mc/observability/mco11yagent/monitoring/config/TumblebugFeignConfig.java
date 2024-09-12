package mcmp.mc.observability.mco11yagent.monitoring.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class TumblebugFeignConfig {
    @Value("${feign.cb-tumblebug.id}")
    private String id;
    @Value("${feign.cb-tumblebug.pw}")
    private String pw;

    @Bean
    public RequestInterceptor tumblebugBasicAuthRequestInterceptor() {
        return requestTemplate -> {
            String auth = id + ":" + pw;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);

            requestTemplate.header("Authorization", authHeader);
        };
    }
}
