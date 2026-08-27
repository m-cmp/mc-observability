package com.mcmp.o11ymanager.manager.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * In-memory monitoring metric cache backed by Caffeine.
 *
 * <p>Three properties define the behaviour:
 *
 * <ul>
 *   <li><b>Freshness follows the query's own aggregation step.</b> The cache key carries a time
 *       bucket whose width comes from {@code group_time}, so a one-minute chart rotates its key
 *       once a minute instead of once an hour.
 *   <li><b>Stale-while-revalidate.</b> Once an entry passes its fresh window it is still returned
 *       immediately while a background task reloads it, so no reader ever waits for InfluxDB just
 *       because a window elapsed, and expiry never produces a synchronised wall of misses.
 *   <li><b>One load per key.</b> Concurrent misses on the same key collapse into a single loader
 *       call through {@link Cache#get(Object, java.util.function.Function)}, so a burst of requests
 *       cannot stampede InfluxDB.
 * </ul>
 *
 * <p>Empty results are cached too, under a short dedicated TTL. Without that, a VM/measurement
 * combination that legitimately has no data re-queried InfluxDB on every single request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringCacheService {

    private final MonitoringCacheProperties properties;
    private final QueryAccessTracker accessTracker;

    private Cache<MonitoringCacheKey, CachedMetrics> cache;
    private ExecutorService refreshExecutor;

    /** Keys with a refresh already in flight, so parallel readers queue at most one reload. */
    private final Map<MonitoringCacheKey, Boolean> refreshing = new ConcurrentHashMap<>();

    private final AtomicLong hitFresh = new AtomicLong();
    private final AtomicLong hitStale = new AtomicLong();
    private final AtomicLong missCold = new AtomicLong();
    private final AtomicLong emptyCached = new AtomicLong();
    private final AtomicLong refreshSubmitted = new AtomicLong();
    private final AtomicLong refreshRejected = new AtomicLong();
    private final AtomicLong refreshFailed = new AtomicLong();
    private final AtomicLong ageSumMillis = new AtomicLong();
    private final AtomicLong ageSamples = new AtomicLong();
    private final AtomicLong ageMaxMillis = new AtomicLong();

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
                                (MonitoringCacheKey key, CachedMetrics value) ->
                                        estimateWeight(key, value, bytesPerPoint))
                        .expireAfterWrite(
                                java.time.Duration.ofSeconds(
                                        Math.max(1L, properties.getHardTtlSeconds())))
                        .recordStats()
                        .build();
        this.refreshExecutor = newRefreshPool(properties.getRefreshThreads());
        log.info(
                "[MON-CACHE] enabled minBucketSec={}, maxBucketSec={},"
                        + " emptyTtlSec={}, hardTtlSec={}, maxWeightMB={}, refreshThreads={}",
                properties.getMinBucketSeconds(),
                properties.getMaxBucketSeconds(),
                properties.getEmptyTtlSeconds(),
                properties.getHardTtlSeconds(),
                properties.getMaxWeightMb(),
                properties.getRefreshThreads());
    }

    @PreDestroy
    void shutdown() {
        if (refreshExecutor != null) {
            refreshExecutor.shutdownNow();
        }
    }

    /**
     * Returns cached metrics for the query, loading them through {@code loader} when nothing usable
     * is cached. See the class javadoc for the freshness and stampede semantics.
     */
    public List<MetricDTO> getOrLoad(
            String nsId,
            String infraId,
            String nodeId,
            MetricRequestDTO req,
            Supplier<List<MetricDTO>> loader) {
        accessTracker.record(nsId, infraId, nodeId);
        if (cache == null) {
            return loader.get();
        }
        MonitoringCacheKey key = keyOf(nsId, infraId, nodeId, req);
        long now = System.currentTimeMillis();

        CachedMetrics hit = cache.getIfPresent(key);
        if (hit != null) {
            recordAge(now - hit.loadedAtMillis());
            if (now < hit.freshUntilMillis()) {
                hitFresh.incrementAndGet();
                return hit.metrics();
            }
            // Past the fresh window but still serviceable: hand back the stale copy right away and
            // refresh it out of band. A reader never waits for InfluxDB because a bucket rolled
            // over.
            hitStale.incrementAndGet();
            submitRefresh(key, loader);
            return hit.metrics();
        }

        missCold.incrementAndGet();
        return loadNow(key, loader);
    }

    /**
     * Loads and stores an entry, collapsing concurrent callers for the same key into one loader
     * invocation.
     */
    private List<MetricDTO> loadNow(MonitoringCacheKey key, Supplier<List<MetricDTO>> loader) {
        CachedMetrics entry = cache.get(key, k -> buildEntry(k, loader));
        return entry == null ? Collections.emptyList() : entry.metrics();
    }

    /** Bypasses the fresh window and reloads the entry — used by the warming scheduler. */
    public List<MetricDTO> refreshNow(
            String nsId,
            String infraId,
            String nodeId,
            MetricRequestDTO req,
            Supplier<List<MetricDTO>> loader) {
        if (cache == null) {
            return loader.get();
        }
        MonitoringCacheKey key = keyOf(nsId, infraId, nodeId, req);
        CachedMetrics entry = buildEntry(key, loader);
        cache.put(key, entry);
        return entry.metrics();
    }

    /** True when the key already holds a fresh entry — lets the warmer skip needless work. */
    public boolean isFresh(String nsId, String infraId, String nodeId, MetricRequestDTO req) {
        if (cache == null) {
            return false;
        }
        CachedMetrics hit = cache.getIfPresent(keyOf(nsId, infraId, nodeId, req));
        return hit != null && System.currentTimeMillis() < hit.freshUntilMillis();
    }

    private MonitoringCacheKey keyOf(
            String nsId, String infraId, String nodeId, MetricRequestDTO req) {
        return MonitoringCacheKey.of(
                nsId,
                infraId,
                nodeId,
                req,
                properties.getMinBucketSeconds(),
                properties.getMaxBucketSeconds());
    }

    private CachedMetrics buildEntry(MonitoringCacheKey key, Supplier<List<MetricDTO>> loader) {
        List<MetricDTO> loaded = loader.get();
        List<MetricDTO> safe = loaded == null ? List.of() : List.copyOf(loaded);
        long now = System.currentTimeMillis();
        boolean empty = !hasAnyDataPoint(safe);
        if (empty) {
            emptyCached.incrementAndGet();
        }
        long freshMillis =
                empty
                        ? Math.max(1L, properties.getEmptyTtlSeconds()) * 1000L
                        : key.freshWindowSeconds() * 1000L;
        return new CachedMetrics(safe, now, now + freshMillis);
    }

    private void submitRefresh(MonitoringCacheKey key, Supplier<List<MetricDTO>> loader) {
        if (refreshExecutor == null) {
            return;
        }
        if (refreshing.putIfAbsent(key, Boolean.TRUE) != null) {
            return; // a refresh for this key is already running
        }
        try {
            refreshExecutor.execute(
                    () -> {
                        try {
                            cache.put(key, buildEntry(key, loader));
                        } catch (Exception e) {
                            refreshFailed.incrementAndGet();
                            log.debug(
                                    "[MON-CACHE] background refresh failed ns={}, mci={}, vm={},"
                                            + " err={}",
                                    key.nsId(),
                                    key.infraId(),
                                    key.nodeId(),
                                    e.toString());
                        } finally {
                            refreshing.remove(key);
                        }
                    });
            refreshSubmitted.incrementAndGet();
        } catch (Exception rejected) {
            refreshing.remove(key);
            refreshRejected.incrementAndGet();
        }
    }

    private void recordAge(long ageMillis) {
        if (ageMillis < 0) {
            return;
        }
        ageSumMillis.addAndGet(ageMillis);
        ageSamples.incrementAndGet();
        ageMaxMillis.accumulateAndGet(ageMillis, Math::max);
    }

    /** Invalidate everything — exposed mainly for ops/admin use. */
    public void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        }
        refreshing.clear();
    }

    /** Returns runtime stats for the {@code /cache/stats} endpoint. */
    public Map<String, Object> stats() {
        if (cache == null) {
            return Map.of("enabled", false);
        }
        CacheStats s = cache.stats();
        long hits = hitFresh.get() + hitStale.get();
        long misses = missCold.get();
        long total = hits + misses;
        long samples = ageSamples.get();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", true);
        out.put("minBucketSeconds", properties.getMinBucketSeconds());
        out.put("maxBucketSeconds", properties.getMaxBucketSeconds());
        out.put("emptyTtlSeconds", properties.getEmptyTtlSeconds());
        out.put("hardTtlSeconds", properties.getHardTtlSeconds());
        out.put("maxWeightMB", properties.getMaxWeightMb());
        out.put("estimatedSize", cache.estimatedSize());
        out.put("hitCount", hits);
        out.put("hitFreshCount", hitFresh.get());
        out.put("hitStaleCount", hitStale.get());
        out.put("missCount", misses);
        out.put("hitRate", total == 0 ? 0d : (double) hits / total);
        out.put("emptyCachedCount", emptyCached.get());
        out.put("refreshSubmittedCount", refreshSubmitted.get());
        out.put("refreshRejectedCount", refreshRejected.get());
        out.put("refreshFailedCount", refreshFailed.get());
        out.put("servedAgeAvgMillis", samples == 0 ? 0L : ageSumMillis.get() / samples);
        out.put("servedAgeMaxMillis", ageMaxMillis.get());
        out.put("evictionCount", s.evictionCount());
        out.put("evictionWeight", s.evictionWeight());
        out.put("trackedVmCount", accessTracker.size());
        return out;
    }

    /** True if at least one series in the list has at least one data point. */
    private static boolean hasAnyDataPoint(List<MetricDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return false;
        }
        for (MetricDTO m : dtos) {
            if (m != null && m.values() != null && !m.values().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static int estimateWeight(
            MonitoringCacheKey key, CachedMetrics value, int bytesPerPoint) {
        int keyBytes =
                (key.nsId().length()
                                        + key.infraId().length()
                                        + key.nodeId().length()
                                        + key.requestSignature().length())
                                * 2
                        + 32;
        List<MetricDTO> metrics = value == null ? null : value.metrics();
        if (metrics == null || metrics.isEmpty()) {
            return keyBytes + 32;
        }
        long points = 0;
        for (MetricDTO m : metrics) {
            if (m == null) {
                continue;
            }
            List<List<Object>> values = m.values();
            if (values != null) {
                points += values.size();
            }
        }
        long weight = (long) keyBytes + points * bytesPerPoint + (long) metrics.size() * 64L;
        return (int) Math.min(weight, Integer.MAX_VALUE);
    }

    private static ExecutorService newRefreshPool(int size) {
        int bounded = Math.max(1, size);
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory =
                r -> {
                    Thread t = new Thread(r, "mon-cache-refresh-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                };
        return Executors.newFixedThreadPool(bounded, factory);
    }

    /** A cached result together with the instant it was loaded and the end of its fresh window. */
    private record CachedMetrics(
            List<MetricDTO> metrics, long loadedAtMillis, long freshUntilMillis) {}
}
