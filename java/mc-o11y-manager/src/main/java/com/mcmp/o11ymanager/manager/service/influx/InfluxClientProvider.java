package com.mcmp.o11ymanager.manager.service.influx;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Keeps one long-lived {@link InfluxDB} client per endpoint (url + credentials).
 *
 * <p>Previously every query built a fresh client through {@code InfluxDBFactory.connect(...)},
 * which allocates a brand-new OkHttp client — its own connection pool, socket, TLS handshake and
 * dispatcher threads — and threw it away right after a single query. Under cache warming that meant
 * over a thousand client creations per minute with zero connection reuse.
 *
 * <p>Clients are cached by endpoint and shared across threads ({@code InfluxDB} is thread-safe), so
 * the underlying connection pool is reused for every query against that endpoint.
 */
@Slf4j
@Component
public class InfluxClientProvider {

    private final Map<String, InfluxDB> clients = new ConcurrentHashMap<>();

    private final int maxIdleConnections;
    private final long keepAliveSeconds;
    private final long connectTimeoutSeconds;
    private final long readTimeoutSeconds;
    private final long writeTimeoutSeconds;

    public InfluxClientProvider(
            @Value("${influx.client.max-idle-connections:32}") int maxIdleConnections,
            @Value("${influx.client.keep-alive-seconds:300}") long keepAliveSeconds,
            @Value("${influx.client.connect-timeout-seconds:5}") long connectTimeoutSeconds,
            @Value("${influx.client.read-timeout-seconds:30}") long readTimeoutSeconds,
            @Value("${influx.client.write-timeout-seconds:30}") long writeTimeoutSeconds) {
        this.maxIdleConnections = maxIdleConnections;
        this.keepAliveSeconds = keepAliveSeconds;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
        this.writeTimeoutSeconds = writeTimeoutSeconds;
    }

    /** Returns the shared client for the given endpoint, creating it on first use. */
    public InfluxDB get(String url, String username, String password) {
        String key = endpointKey(url, username);
        return clients.computeIfAbsent(key, k -> create(url, username, password));
    }

    private InfluxDB create(String url, String username, String password) {
        OkHttpClient.Builder http =
                new OkHttpClient.Builder()
                        .connectionPool(
                                new ConnectionPool(
                                        maxIdleConnections, keepAliveSeconds, TimeUnit.SECONDS))
                        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                        .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true);
        log.info("[INFLUX-CLIENT] creating shared client for url={}, user={}", url, username);
        return InfluxDBFactory.connect(url, username, password, http);
    }

    private static String endpointKey(String url, String username) {
        return Objects.toString(url, "") + "|" + Objects.toString(username, "");
    }

    /** Drops every cached client. Exposed for ops/admin and tests. */
    public void closeAll() {
        clients.values()
                .forEach(
                        c -> {
                            try {
                                c.close();
                            } catch (Exception e) {
                                log.debug("[INFLUX-CLIENT] close failed: {}", e.toString());
                            }
                        });
        clients.clear();
    }

    /** Number of live shared clients — surfaced in cache stats. */
    public int size() {
        return clients.size();
    }

    @PreDestroy
    void shutdown() {
        closeAll();
    }
}
