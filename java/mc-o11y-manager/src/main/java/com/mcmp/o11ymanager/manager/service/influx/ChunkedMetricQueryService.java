package com.mcmp.o11ymanager.manager.service.influx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mcmp.o11ymanager.manager.config.ChunkCacheProperties;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Executes a metric query as a set of absolute time slices, reusing the ones that cannot change.
 *
 * <p>See {@link ChunkedMetricQuery} for why slicing works and when it is refused. This class adds
 * the cache and the actual query execution around that logic.
 *
 * <p>Disabled by default. It reduces how much data InfluxDB scans in the background; it does not
 * change user-visible latency, which the result cache and its stale-while-revalidate path already
 * handle. Turn it on where InfluxDB load is actually a problem, and confirm with the counters this
 * class exposes.
 */
@Slf4j
@Component
public class ChunkedMetricQueryService {

    private final ChunkCacheProperties properties;
    private final Cache<String, List<MetricDTO>> sliceCache;

    private final AtomicLong plansChunked = new AtomicLong();
    private final AtomicLong plansRejected = new AtomicLong();
    private final AtomicLong sliceHits = new AtomicLong();
    private final AtomicLong sliceMisses = new AtomicLong();
    private final AtomicLong sliceUncacheable = new AtomicLong();
    private final AtomicLong fallbacks = new AtomicLong();

    public ChunkedMetricQueryService(ChunkCacheProperties properties) {
        this.properties = properties;
        this.sliceCache =
                Caffeine.newBuilder()
                        .maximumSize(Math.max(64, properties.getMaxSlices()))
                        .expireAfterWrite(
                                Duration.ofSeconds(Math.max(60, properties.getSliceTtlSeconds())))
                        .build();
    }

    /**
     * Runs the query, slicing it when that is provably equivalent and falling back to {@code
     * wholeWindow} otherwise.
     *
     * @param sliceExecutor runs one absolute-window query and returns its series
     * @param wholeWindow the original single-query path, used whenever slicing is refused
     */
    public List<MetricDTO> fetch(
            InfluxDTO dto,
            String retentionPolicy,
            MetricRequestDTO req,
            Function<String, List<MetricDTO>> sliceExecutor,
            java.util.function.Supplier<List<MetricDTO>> wholeWindow) {
        if (!properties.isEnabled()) {
            return wholeWindow.get();
        }

        ChunkedMetricQuery.Plan plan;
        try {
            plan =
                    ChunkedMetricQuery.plan(
                            req,
                            System.currentTimeMillis() / 1000L,
                            properties.getTargetChunks(),
                            properties.getMaxChunks(),
                            properties.getMinRangeSeconds(),
                            settleSeconds(req));
        } catch (Exception e) {
            log.debug("[CHUNK] planning failed, falling back: {}", e.toString());
            fallbacks.incrementAndGet();
            return wholeWindow.get();
        }

        if (!plan.applicable()) {
            plansRejected.incrementAndGet();
            return wholeWindow.get();
        }

        try {
            List<List<MetricDTO>> results = new ArrayList<>(plan.slices().size());
            for (ChunkedMetricQuery.Slice slice : plan.slices()) {
                results.add(runSlice(dto, retentionPolicy, req, slice, sliceExecutor));
            }
            plansChunked.incrementAndGet();
            return ChunkedMetricQuery.merge(results, req.getLimit());
        } catch (Exception e) {
            // Any surprise here must not turn into a broken graph — take the original path.
            log.warn(
                    "[CHUNK] slice execution failed, falling back to single query: {}",
                    e.toString());
            fallbacks.incrementAndGet();
            return wholeWindow.get();
        }
    }

    private List<MetricDTO> runSlice(
            InfluxDTO dto,
            String retentionPolicy,
            MetricRequestDTO req,
            ChunkedMetricQuery.Slice slice,
            Function<String, List<MetricDTO>> sliceExecutor) {
        String query =
                com.mcmp.o11ymanager.manager.model.influx.InfluxQl.buildRangeQuery(
                        req, retentionPolicy, slice.startNanos(), slice.endNanos());
        if (!slice.cacheable()) {
            sliceUncacheable.incrementAndGet();
            return sliceExecutor.apply(query);
        }
        String key = sliceKey(dto, retentionPolicy, req, slice);
        List<MetricDTO> hit = sliceCache.getIfPresent(key);
        if (hit != null) {
            sliceHits.incrementAndGet();
            return hit;
        }
        sliceMisses.incrementAndGet();
        List<MetricDTO> loaded =
                sliceCache.get(
                        key,
                        k -> {
                            List<MetricDTO> r = sliceExecutor.apply(query);
                            return r == null ? List.of() : List.copyOf(r);
                        });
        return loaded == null ? List.of() : loaded;
    }

    /**
     * How long a slice must have been closed before it is considered stable. Defaults to one
     * aggregation step, which covers the usual late-arrival window of an agent write.
     */
    private long settleSeconds(MetricRequestDTO req) {
        long configured = properties.getSettleSeconds();
        if (configured > 0) {
            return configured;
        }
        return Math.max(1L, ChunkedMetricQuery.parseSeconds(req.getGroupTime()));
    }

    private static String sliceKey(
            InfluxDTO dto,
            String retentionPolicy,
            MetricRequestDTO req,
            ChunkedMetricQuery.Slice slice) {
        StringBuilder sb = new StringBuilder(160);
        sb.append(dto.getUrl())
                .append('|')
                .append(dto.getDatabase())
                .append('|')
                .append(retentionPolicy == null ? "" : retentionPolicy)
                .append('|')
                .append(req.getMeasurement())
                .append('|')
                .append(req.getGroupTime())
                .append('|')
                .append(slice.chunkStartSeconds())
                .append('|')
                .append(slice.startNanos())
                .append('-')
                .append(slice.endNanos())
                .append('|');
        if (req.getFields() != null) {
            List<String> rendered = new ArrayList<>();
            for (MetricRequestDTO.FieldInfo f : req.getFields()) {
                if (f != null) {
                    rendered.add(f.getFunction() + ":" + f.getField());
                }
            }
            rendered.sort(String::compareTo);
            sb.append(String.join(",", rendered)).append('|');
        }
        if (req.getGroupBy() != null) {
            List<String> gb = new ArrayList<>(req.getGroupBy());
            gb.sort(String::compareTo);
            sb.append(String.join(",", gb)).append('|');
        }
        if (req.getConditions() != null) {
            List<String> conds = new ArrayList<>();
            for (MetricRequestDTO.ConditionInfo c : req.getConditions()) {
                if (c != null && c.getKey() != null) {
                    conds.add(c.getKey() + "=" + c.getValue());
                }
            }
            conds.sort(String::compareTo);
            sb.append(String.join(",", conds));
        }
        return sb.toString();
    }

    /** Counters surfaced through the cache stats endpoint. */
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", properties.isEnabled());
        out.put("targetChunks", properties.getTargetChunks());
        out.put("sliceEntries", sliceCache.estimatedSize());
        out.put("plansChunked", plansChunked.get());
        out.put("plansRejected", plansRejected.get());
        out.put("sliceHits", sliceHits.get());
        out.put("sliceMisses", sliceMisses.get());
        out.put("sliceUncacheable", sliceUncacheable.get());
        out.put("fallbacks", fallbacks.get());
        return out;
    }

    public void invalidateAll() {
        sliceCache.invalidateAll();
    }
}
