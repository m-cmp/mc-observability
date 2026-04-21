package com.mcmp.o11ymanager.manager.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.mcmp.o11ymanager.manager.config.CspCacheProperties;
import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * In-memory cache for cb-spider monitoring responses.
 *
 * <p>The NS/MCI overview in the frontend fans out to 8 metrics × N VMs on each tab visit. Without a
 * cache, every tab switch re-hits cb-spider which synchronously fans out to the CSP provider APIs —
 * a slow round trip. This cache absorbs those repeated calls within a short TTL window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CspCacheService {

    private final CspCacheProperties properties;

    private Cache<CspCacheKey, SpiderMonitoringInfo.Data> cache;
    private final AtomicLong manualHitCount = new AtomicLong();
    private final AtomicLong manualMissCount = new AtomicLong();

    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            log.info("[CSP-CACHE] disabled by configuration");
            return;
        }
        this.cache =
                Caffeine.newBuilder()
                        .maximumSize(properties.getMaxSize())
                        .expireAfterWrite(
                                Duration.ofSeconds(properties.getExpireAfterWriteSeconds()))
                        .recordStats()
                        .build();
        log.info(
                "[CSP-CACHE] enabled maxSize={}, ttlSec={}",
                properties.getMaxSize(),
                properties.getExpireAfterWriteSeconds());
    }

    /** Returns cached monitoring data or loads it via {@code loader} on miss. */
    public SpiderMonitoringInfo.Data getOrLoad(
            CspCacheKey key, Supplier<SpiderMonitoringInfo.Data> loader) {
        if (cache == null) {
            return loader.get();
        }
        SpiderMonitoringInfo.Data hit = cache.getIfPresent(key);
        if (hit != null) {
            manualHitCount.incrementAndGet();
            return hit;
        }
        manualMissCount.incrementAndGet();
        SpiderMonitoringInfo.Data loaded = loader.get();
        if (loaded != null) {
            cache.put(key, loaded);
        }
        return loaded;
    }

    /** Direct put — used by warmers that already hold a freshly-fetched response. */
    public void put(CspCacheKey key, SpiderMonitoringInfo.Data value) {
        if (cache != null && value != null) {
            cache.put(key, value);
        }
    }

    public void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    public Map<String, Object> stats() {
        if (cache == null) {
            return Map.of("enabled", false);
        }
        CacheStats s = cache.stats();
        long total = manualHitCount.get() + manualMissCount.get();
        double hitRate = total == 0 ? 0d : (double) manualHitCount.get() / total;
        return Map.ofEntries(
                Map.entry("enabled", true),
                Map.entry("maxSize", properties.getMaxSize()),
                Map.entry("expireAfterWriteSeconds", properties.getExpireAfterWriteSeconds()),
                Map.entry("estimatedSize", cache.estimatedSize()),
                Map.entry("hitCount", manualHitCount.get()),
                Map.entry("missCount", manualMissCount.get()),
                Map.entry("hitRate", hitRate),
                Map.entry("evictionCount", s.evictionCount()));
    }
}
