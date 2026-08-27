package com.mcmp.o11ymanager.manager.service.influx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Caches the auxiliary InfluxDB lookups that used to run on every single metric query.
 *
 * <p>A cache miss on the metric path used to cost three or four InfluxDB round trips: retention
 * policy lookup, an existence probe for the ns/mci/vm tag combination, an optional probe against
 * the downsampling database for range routing, and only then the actual data query. The first three
 * change very rarely, so they are memoised here and the metric path drops to a single round trip.
 *
 * <p>Retention policies get a long TTL (they are effectively static). Existence probes get a short
 * TTL so that a newly registered VM shows up quickly. Negative existence results get an even
 * shorter TTL so a VM that has just started reporting is not hidden for the full window.
 */
@Slf4j
@Component
public class InfluxMetaCache {

    private final Cache<String, Optional<String>> retentionPolicyCache;
    private final Cache<String, Boolean> existsCache;
    private final Cache<String, List<String>> measurementCache;

    private final long existsNegativeTtlSeconds;

    private final AtomicLong rpHits = new AtomicLong();
    private final AtomicLong rpMisses = new AtomicLong();
    private final AtomicLong existsHits = new AtomicLong();
    private final AtomicLong existsMisses = new AtomicLong();

    public InfluxMetaCache(
            @Value("${influx.meta.retention-policy-ttl-seconds:3600}") long rpTtlSeconds,
            @Value("${influx.meta.exists-ttl-seconds:60}") long existsTtlSeconds,
            @Value("${influx.meta.exists-negative-ttl-seconds:15}") long existsNegativeTtlSeconds,
            @Value("${influx.meta.measurement-ttl-seconds:600}") long measurementTtlSeconds,
            @Value("${influx.meta.max-size:20000}") long maxSize) {
        this.existsNegativeTtlSeconds = existsNegativeTtlSeconds;
        this.retentionPolicyCache =
                Caffeine.newBuilder()
                        .maximumSize(Math.max(64, maxSize / 100))
                        .expireAfterWrite(Duration.ofSeconds(rpTtlSeconds))
                        .build();
        this.existsCache =
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfter(
                                new com.github.benmanes.caffeine.cache.Expiry<String, Boolean>() {
                                    @Override
                                    public long expireAfterCreate(
                                            String key, Boolean value, long currentTime) {
                                        long sec =
                                                Boolean.TRUE.equals(value)
                                                        ? existsTtlSeconds
                                                        : existsNegativeTtlSeconds;
                                        return Duration.ofSeconds(Math.max(1, sec)).toNanos();
                                    }

                                    @Override
                                    public long expireAfterUpdate(
                                            String key,
                                            Boolean value,
                                            long currentTime,
                                            long currentDuration) {
                                        return expireAfterCreate(key, value, currentTime);
                                    }

                                    @Override
                                    public long expireAfterRead(
                                            String key,
                                            Boolean value,
                                            long currentTime,
                                            long currentDuration) {
                                        return currentDuration;
                                    }
                                })
                        .build();
        this.measurementCache =
                Caffeine.newBuilder()
                        .maximumSize(64)
                        .expireAfterWrite(Duration.ofSeconds(measurementTtlSeconds))
                        .build();
        log.info(
                "[INFLUX-META] enabled rpTtlSec={}, existsTtlSec={}, existsNegTtlSec={}, measurementTtlSec={}",
                rpTtlSeconds,
                existsTtlSeconds,
                existsNegativeTtlSeconds,
                measurementTtlSeconds);
    }

    /** Memoised retention-policy lookup for a (url, database) pair. */
    public String retentionPolicy(String url, String database, Supplier<String> loader) {
        String key = url + "|" + database;
        Optional<String> cached = retentionPolicyCache.getIfPresent(key);
        if (cached != null) {
            rpHits.incrementAndGet();
            return cached.orElse(null);
        }
        rpMisses.incrementAndGet();
        Optional<String> resolved =
                retentionPolicyCache.get(key, k -> Optional.ofNullable(loader.get()));
        return resolved == null ? null : resolved.orElse(null);
    }

    /** Memoised existence probe for a tag combination inside one database. */
    public boolean exists(String probeKey, BooleanSupplier loader) {
        Boolean cached = existsCache.getIfPresent(probeKey);
        if (cached != null) {
            existsHits.incrementAndGet();
            return cached;
        }
        existsMisses.incrementAndGet();
        Boolean resolved = existsCache.get(probeKey, k -> loader.getAsBoolean());
        return Boolean.TRUE.equals(resolved);
    }

    /** Memoised measurement-name discovery, shared by every warming tick. */
    public List<String> measurements(String scope, Supplier<List<String>> loader) {
        List<String> resolved =
                measurementCache.get(
                        scope,
                        k -> {
                            List<String> loaded = loader.get();
                            return loaded == null ? List.of() : List.copyOf(loaded);
                        });
        return resolved == null ? List.of() : resolved;
    }

    /** Builds the canonical key for an existence probe. */
    public static String probeKey(String url, String database, String... parts) {
        StringBuilder sb = new StringBuilder(96);
        sb.append(url).append('|').append(database);
        for (String p : parts) {
            sb.append('|').append(p == null ? "" : p);
        }
        return sb.toString();
    }

    public void invalidateAll() {
        retentionPolicyCache.invalidateAll();
        existsCache.invalidateAll();
        measurementCache.invalidateAll();
    }

    /** Runtime counters surfaced through the cache stats endpoint. */
    public Map<String, Object> stats() {
        return Map.of(
                "retentionPolicyEntries", retentionPolicyCache.estimatedSize(),
                "retentionPolicyHits", rpHits.get(),
                "retentionPolicyMisses", rpMisses.get(),
                "existsEntries", existsCache.estimatedSize(),
                "existsHits", existsHits.get(),
                "existsMisses", existsMisses.get(),
                "existsNegativeTtlSeconds", existsNegativeTtlSeconds,
                "measurementEntries", measurementCache.estimatedSize());
    }
}
