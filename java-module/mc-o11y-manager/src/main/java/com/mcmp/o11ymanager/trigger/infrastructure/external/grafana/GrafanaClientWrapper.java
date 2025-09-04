package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Utility wrapper class for Grafana API client operations Provides centralized error handling and
 * logging for WebClient HTTP responses. Catches WebClientResponseException and logs detailed error
 * information before re-throwing.
 */
public class GrafanaClientWrapper {
    private static final Logger log = LoggerFactory.getLogger(GrafanaClientWrapper.class);

    /**
     * Executes a Grafana API call that returns a value with error handling.
     *
     * @param supplier function that performs the HTTP call and returns a result
     * @param <T> type of the return value
     * @return result from the supplier function
     * @throws WebClientResponseException if the HTTP call fails, after logging error details
     */
    static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (WebClientResponseException e) {
            logFor(e);
            throw e;
        }
    }

    /**
     * Executes a Grafana API call with no return value with error handling.
     *
     * @param runnable function that performs the HTTP call
     * @throws WebClientResponseException if the HTTP call fails, after logging error details
     */
    static void call(Runnable runnable) {
        try {
            runnable.run();
        } catch (WebClientResponseException e) {
            logFor(e);
            throw e;
        }
    }

    /**
     * Logs detailed error information from Grafana API failures.
     *
     * @param e WebClientResponseException containing HTTP error details
     */
    private static void logFor(WebClientResponseException e) {
        log.error(
                "Grafana API Error, StatusCode: {}, Body: {}",
                e.getStatusCode(),
                e.getResponseBodyAsString());
    }
}
