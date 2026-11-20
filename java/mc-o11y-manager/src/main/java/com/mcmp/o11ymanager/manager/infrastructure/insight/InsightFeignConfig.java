package com.mcmp.o11ymanager.manager.infrastructure.insight;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Feign settings for the insight client only (not a @Configuration, so it does not become the
 * default of every client).
 *
 * <p>Log and alert analysis answer only after the LLM has finished, which takes minutes. A retry
 * would start the same analysis again on insight while the first one is still running, so a
 * timed-out call is not retried. The read timeout is set per client in application.yaml
 * (feign.client.config.insight).
 *
 * <p>{@code TumblebugFeignConfig} is a @Configuration, so its retryer is visible to every client;
 * {@code @Primary} makes this one win for the insight client.
 */
public class InsightFeignConfig {

    @Bean
    @Primary
    public Retryer insightRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
