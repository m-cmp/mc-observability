package com.mcmp.o11ymanager.manager.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * In-memory monitoring metric cache backed by Caffeine.
 *
 * <p>Results are bucketed by 1-hour wall-clock blocks (configurable). Within the same hour,
 * identical requests return cached data without touching InfluxDB. Eviction is driven by total
 * weight (default 512MB) and a 7-day TTL — naturally retaining the most recently queried VMs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringCacheService {

    private final MonitoringCacheProperties properties;
    private final VmCreatedTimeResolver vmCreatedTimeResolver;

    private Cache<MonitoringCacheKey, List<MetricDTO>> cache;
    private final AtomicLong manualHitCount = new AtomicLong();
    private final AtomicLong manualMissCount = new AtomicLong();
    private final AtomicLong skippedTooOldCount = new AtomicLong();

    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            log.info("[MON-CACHE] disabled by configuration");
            return;
        }
        long maxWeightBytes = properties.getMaxWeightMb() * 1024L * 1024L;
        int bytesPerPoint = Math.max(1, properties.getEstimatedBytesPerPoint());

        this.cache =
                Caffeine.newBuilder()
                        .maximumWeight(maxWeightBytes)
                        .weigher(
                                (MonitoringCacheKey key, List<MetricDTO> value) ->
                                        estimateWeight(key, value, bytesPerPoint))
                        .expireAfter(buildExpiry())
                        .recordStats()
                        .build();
        log.info(
                "[MON-CACHE] enabled blockPeriodSec={}, maxWeightMB={}, ttlSec={}",
                properties.getBlockPeriodSeconds(),
                properties.getMaxWeightMb(),
                properties.getExpireAfterWriteSeconds());
    }

    /**
     * Returns the cached metric list for the given query, or computes it from InfluxDB via {@code
     * loader} on miss and stores the result.
     */
    public List<MetricDTO> getOrLoad(
            String nsId,
            String mciId,
            String vmId,
            MetricRequestDTO req,
            Supplier<List<MetricDTO>> loader) {
        if (cache == null) {
            return loader.get();
        }
        MonitoringCacheKey key =
                MonitoringCacheKey.of(nsId, mciId, vmId, req, properties.getBlockPeriodSeconds());

        List<MetricDTO> hit = cache.getIfPresent(key);
        if (hit != null) {
            manualHitCount.incrementAndGet();
            log.debug(
                    "[MON-CACHE] HIT ns={}, mci={}, vm={}, bucket={}",
                    key.nsId(),
                    key.mciId(),
                    key.vmId(),
                    key.hourBucket());
            return hit;
        }

        manualMissCount.incrementAndGet();
        log.debug(
                "[MON-CACHE] MISS ns={}, mci={}, vm={}, bucket={}",
                key.nsId(),
                key.mciId(),
                key.vmId(),
                key.hourBucket());

        List<MetricDTO> loaded = loader.get();
        if (loaded == null) {
            return Collections.emptyList();
        }
        // Only cache when the VM was created within the last 7 days. Anything older has nothing
        // useful left to cache (would expire immediately) and would just waste a slot.
        if (computeTtlNanos(key) > 0) {
            cache.put(key, loaded);
        } else {
            skippedTooOldCount.incrementAndGet();
        }
        return loaded;
    }

    /** Invalidate everything — exposed mainly for ops/admin use. */
    public void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    /** Returns runtime stats for the {@code /cache/stats} endpoint. */
    public Map<String, Object> stats() {
        if (cache == null) {
            return Map.of("enabled", false);
        }
        CacheStats s = cache.stats();
        long totalRequests = manualHitCount.get() + manualMissCount.get();
        double hitRate = totalRequests == 0 ? 0d : (double) manualHitCount.get() / totalRequests;
        return Map.ofEntries(
                Map.entry("enabled", true),
                Map.entry("blockPeriodSeconds", properties.getBlockPeriodSeconds()),
                Map.entry("maxWeightMB", properties.getMaxWeightMb()),
                Map.entry("expireAfterWriteSeconds", properties.getExpireAfterWriteSeconds()),
                Map.entry("estimatedSize", cache.estimatedSize()),
                Map.entry("hitCount", manualHitCount.get()),
                Map.entry("missCount", manualMissCount.get()),
                Map.entry("hitRate", hitRate),
                Map.entry("evictionCount", s.evictionCount()),
                Map.entry("evictionWeight", s.evictionWeight()),
                Map.entry("loadFailureCount", s.loadFailureCount()),
                Map.entry("skippedTooOldCount", skippedTooOldCount.get()));
    }

    /**
     * Returns the remaining lifetime (in nanoseconds) for an entry whose VM was created at the
     * resolved {@code createdTime}. Falls back to the configured global TTL when Tumblebug doesn't
     * expose a creation time. Returns {@code 0} when the VM is already older than the configured
     * window — the caller should skip caching such entries.
     */
    private long computeTtlNanos(MonitoringCacheKey key) {
        long maxTtlSec = properties.getExpireAfterWriteSeconds();
        if (key.vmId() == null || key.vmId().isEmpty()) {
            // ns/mci-scoped queries can't be tied to a specific VM creation time → use global TTL
            return TimeUnit.SECONDS.toNanos(maxTtlSec);
        }
        Optional<Instant> createdAt =
                vmCreatedTimeResolver.resolve(key.nsId(), key.mciId(), key.vmId());
        if (createdAt.isEmpty()) {
            return TimeUnit.SECONDS.toNanos(maxTtlSec);
        }
        Instant deadline = createdAt.get().plusSeconds(maxTtlSec);
        long remainSec = Duration.between(Instant.now(), deadline).getSeconds();
        if (remainSec <= 0) {
            return 0L;
        }
        return TimeUnit.SECONDS.toNanos(Math.min(remainSec, maxTtlSec));
    }

    private Expiry<MonitoringCacheKey, List<MetricDTO>> buildExpiry() {
        return new Expiry<>() {
            @Override
            public long expireAfterCreate(
                    MonitoringCacheKey key, List<MetricDTO> value, long currentTime) {
                return Math.max(1L, computeTtlNanos(key));
            }

            @Override
            public long expireAfterUpdate(
                    MonitoringCacheKey key,
                    List<MetricDTO> value,
                    long currentTime,
                    long currentDuration) {
                return Math.max(1L, computeTtlNanos(key));
            }

            @Override
            public long expireAfterRead(
                    MonitoringCacheKey key,
                    List<MetricDTO> value,
                    long currentTime,
                    long currentDuration) {
                // reads do not extend the lifetime — keep the createdTime-bound deadline
                return currentDuration;
            }
        };
    }

    private static int estimateWeight(
            MonitoringCacheKey key, List<MetricDTO> value, int bytesPerPoint) {
        int keyBytes =
                (key.nsId().length()
                                        + key.mciId().length()
                                        + key.vmId().length()
                                        + key.requestSignature().length())
                                * 2
                        + 16;
        if (value == null || value.isEmpty()) {
            return keyBytes + 32;
        }
        long points = 0;
        for (MetricDTO m : value) {
            if (m == null) {
                continue;
            }
            List<List<Object>> values = m.values();
            if (values != null) {
                points += values.size();
            }
        }
        long weight = (long) keyBytes + points * bytesPerPoint + (long) value.size() * 64L;
        return (int) Math.min(weight, Integer.MAX_VALUE);
    }
}
