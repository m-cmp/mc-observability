package com.mcmp.o11ymanager.manager.service.influx;

import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Splits a relative-range metric query into absolute, step-aligned time slices.
 *
 * <p>A dashboard asks for "the last hour, bucketed by minute". One minute later it asks again — and
 * 59 of those 60 buckets are byte-for-byte the same, because <b>a time bucket that lies entirely in
 * the past can never change</b>. Re-reading the whole hour every minute throws that away.
 *
 * <p>This class expresses the same request as a set of absolute windows aligned to the aggregation
 * step. Slices that are complete and settled are stable and can be cached; only the slices touching
 * the edges of the requested window have to be read again.
 *
 * <p>This type holds the planning and merging logic only — no I/O — so the arithmetic can be tested
 * directly. {@link ChunkedMetricQueryService} performs the actual queries.
 */
public final class ChunkedMetricQuery {

    private ChunkedMetricQuery() {}

    /** One absolute slice of the requested window. */
    public record Slice(
            long startNanos, long endNanos, boolean cacheable, long chunkStartSeconds) {}

    /**
     * A planned decomposition of a request, or {@link #notApplicable()} when chunking is refused.
     */
    public record Plan(List<Slice> slices, long alignedEndSeconds, long startSeconds) {

        public boolean applicable() {
            return !slices.isEmpty();
        }

        public static Plan notApplicable() {
            return new Plan(List.of(), 0L, 0L);
        }
    }

    /**
     * Decides whether the request can be chunked, and if so how.
     *
     * <p>Chunking is refused — falling back to a single query — whenever the result would not be
     * provably identical:
     *
     * <ul>
     *   <li>no aggregate function, so no {@code GROUP BY time()} and points land on arbitrary
     *       timestamps rather than deterministic buckets;
     *   <li>no usable step or range;
     *   <li>a {@code limit} that could actually truncate, since InfluxDB applies it per query and
     *       the per-slice results would each be truncated separately;
     *   <li>a window so short that slicing buys nothing, or so long that it would produce more
     *       slices than allowed.
     * </ul>
     *
     * @param nowSeconds current wall clock, injected so the planning is testable
     */
    public static Plan plan(
            MetricRequestDTO req,
            long nowSeconds,
            int targetChunks,
            int maxChunks,
            long minRangeSeconds,
            long settleSeconds) {
        if (req == null || !hasAggregate(req)) {
            return Plan.notApplicable();
        }
        long step = parseSeconds(req.getGroupTime());
        long range = parseSeconds(req.getRange());
        if (step <= 0 || range <= 0 || range < minRangeSeconds || range <= step) {
            return Plan.notApplicable();
        }

        long expectedPoints = range / step;
        Long limit = req.getLimit();
        if (limit != null && limit > 0 && limit < expectedPoints) {
            // The limit would cut the result short; slicing would apply it to each slice instead.
            return Plan.notApplicable();
        }

        long alignedEnd = floorTo(nowSeconds, step);
        long start = alignedEnd - range;
        if (start >= alignedEnd) {
            return Plan.notApplicable();
        }

        long chunkWidth = chunkWidth(range, step, targetChunks, maxChunks);
        if (chunkWidth <= 0 || chunkWidth >= range) {
            return Plan.notApplicable();
        }

        List<Slice> slices = new ArrayList<>();
        long chunkStart = floorTo(start, chunkWidth);
        while (chunkStart < alignedEnd) {
            long chunkEnd = chunkStart + chunkWidth;
            long lo = Math.max(chunkStart, start);
            long hi = Math.min(chunkEnd, alignedEnd);
            if (lo < hi) {
                boolean full = lo == chunkStart && hi == chunkEnd;
                // Only a complete slice that has had time to settle is stable enough to keep.
                // Late-arriving writes land in the most recent buckets, so a slice that only just
                // closed is deliberately not cached.
                boolean cacheable = full && (chunkEnd + settleSeconds <= nowSeconds);
                slices.add(new Slice(toNanos(lo), toNanos(hi), cacheable, chunkStart));
            }
            chunkStart = chunkEnd;
        }
        if (slices.size() < 2) {
            return Plan.notApplicable();
        }
        return new Plan(List.copyOf(slices), alignedEnd, start);
    }

    /**
     * Merges per-slice results into one response.
     *
     * <p>Slices are disjoint and each is already ordered newest-first, so walking them from newest
     * to oldest and concatenating preserves the global ordering without having to parse a single
     * timestamp.
     */
    public static List<MetricDTO> merge(List<List<MetricDTO>> sliceResultsOldestFirst, Long limit) {
        Map<SeriesKey, MetricDTO> heads = new LinkedHashMap<>();
        Map<SeriesKey, List<List<Object>>> rows = new LinkedHashMap<>();

        for (int i = sliceResultsOldestFirst.size() - 1; i >= 0; i--) {
            List<MetricDTO> slice = sliceResultsOldestFirst.get(i);
            if (slice == null) {
                continue;
            }
            for (MetricDTO dto : slice) {
                if (dto == null) {
                    continue;
                }
                SeriesKey key = SeriesKey.of(dto);
                heads.putIfAbsent(key, dto);
                List<List<Object>> target = rows.computeIfAbsent(key, k -> new ArrayList<>());
                if (dto.values() != null) {
                    target.addAll(dto.values());
                }
            }
        }

        List<MetricDTO> out = new ArrayList<>(heads.size());
        for (Map.Entry<SeriesKey, MetricDTO> e : heads.entrySet()) {
            MetricDTO head = e.getValue();
            List<List<Object>> values = rows.getOrDefault(e.getKey(), List.of());
            if (limit != null && limit > 0 && values.size() > limit) {
                values = new ArrayList<>(values.subList(0, limit.intValue()));
            }
            out.add(new MetricDTO(head.name(), head.columns(), head.tags(), values));
        }
        return out;
    }

    /** Identifies one time series across slices: same measurement name and same tag set. */
    private record SeriesKey(String name, Map<String, String> tags) {
        static SeriesKey of(MetricDTO dto) {
            return new SeriesKey(
                    Objects.toString(dto.name(), ""),
                    dto.tags() == null ? Map.of() : new LinkedHashMap<>(dto.tags()));
        }
    }

    /**
     * Slice width: the requested range divided into roughly {@code targetChunks} parts, rounded to
     * a whole number of steps so that no aggregation bucket ever straddles a slice boundary.
     */
    static long chunkWidth(long range, long step, int targetChunks, int maxChunks) {
        int target = Math.max(2, targetChunks);
        long raw = Math.max(step, range / target);
        long width = Math.max(step, (raw / step) * step);
        int cap = Math.max(2, maxChunks);
        while (range / width > cap) {
            width += step;
        }
        return width;
    }

    static long floorTo(long value, long unit) {
        if (unit <= 0) {
            return value;
        }
        return Math.floorDiv(value, unit) * unit;
    }

    private static long toNanos(long seconds) {
        return seconds * 1_000_000_000L;
    }

    private static boolean hasAggregate(MetricRequestDTO req) {
        if (req.getFields() == null || req.getFields().isEmpty()) {
            return false;
        }
        for (MetricRequestDTO.FieldInfo f : req.getFields()) {
            if (f != null && f.getFunction() != null && !f.getFunction().isBlank()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses an InfluxQL-style duration such as {@code 30s}, {@code 5m}, {@code 1h}, {@code 7d}.
     */
    static long parseSeconds(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        String s = value.trim().toLowerCase();
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i == 0 || i == s.length()) {
            return 0L;
        }
        long n;
        try {
            n = Long.parseLong(s.substring(0, i));
        } catch (NumberFormatException e) {
            return 0L;
        }
        return switch (s.substring(i)) {
            case "s" -> n;
            case "m" -> n * 60L;
            case "h" -> n * 3600L;
            case "d" -> n * 86400L;
            case "w" -> n * 604800L;
            default -> 0L;
        };
    }
}
