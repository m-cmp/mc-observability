package mcmp.mc.observability.mco11yagent.monitoring.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class SpiderFeignConfig {
    @Value("${feign.cb-spider.id}")
    private String id;
    @Value("${feign.cb-spider.pw}")
    private String pw;

    @Bean
    public RequestInterceptor spiderBasicAuthRequestInterceptor() {
        return requestTemplate -> {
            if (authRequired()) {
                String auth = id + ":" + pw;
                byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
                String authHeader = "Basic " + new String(encodedAuth);
                requestTemplate.header("Authorization", authHeader);
            }
        };
    }

    private boolean authRequired() {
        return id != null && pw != null && !id.isEmpty() && !pw.isEmpty();
    }
}
