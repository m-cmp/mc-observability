package com.mcmp.o11ymanager.manager.service.cache;

import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Cache key for monitoring metric query results.
 *
 * <p>The key deliberately carries <b>no wall-clock component</b>. One query against one VM maps to
 * a single entry whose contents are refreshed over time, rather than to a new entry per time
 * window.
 *
 * <p>Quantising time into the key produced a synchronised wall of misses every time the window
 * rolled over, and with a one-hour window a one-minute chart could be served from data 59 minutes
 * old. Freshness is now a lifetime on the entry instead: {@link #freshWindowSeconds()} is the
 * query's own {@code group_time} step, so an entry stays fresh for exactly as long as it takes a
 * new data point to exist, and past that it is refreshed in the background while still being
 * served.
 */
public record MonitoringCacheKey(
        String nsId,
        String infraId,
        String nodeId,
        String requestSignature,
        long freshWindowSeconds) {

    /** Builds the canonical cache key for a metric query. */
    public static MonitoringCacheKey of(
            String nsId,
            String infraId,
            String nodeId,
            MetricRequestDTO req,
            long minBucketSeconds,
            long maxBucketSeconds) {
        return new MonitoringCacheKey(
                nullToEmpty(nsId),
                nullToEmpty(infraId),
                nullToEmpty(nodeId),
                signatureOf(req),
                freshWindowOf(req, minBucketSeconds, maxBucketSeconds));
    }

    /**
     * Fresh window for a request: its {@code group_time} step, clamped to the configured floor and
     * ceiling. Requests without a usable step fall back to the floor, which is the safe choice —
     * too short a window only costs an extra background reload, while too long a one serves stale
     * data.
     */
    static long freshWindowOf(MetricRequestDTO req, long minBucketSeconds, long maxBucketSeconds) {
        long floor = Math.max(1L, minBucketSeconds);
        long ceiling = Math.max(floor, maxBucketSeconds);
        long step = req == null ? 0L : parseDurationSeconds(req.getGroupTime());
        if (step <= 0L) {
            return floor;
        }
        return Math.min(Math.max(step, floor), ceiling);
    }

    /**
     * Parses an InfluxQL-style duration such as {@code "30s"}, {@code "1m"}, {@code "1h"} or {@code
     * "7d"} into seconds. Returns {@code 0} when the value is absent or unparseable.
     */
    static long parseDurationSeconds(String value) {
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

    /**
     * Stable signature for a {@link MetricRequestDTO} that ignores ns_id/infra_id/node_id
     * conditions (those live in their own key fields) and is order-insensitive for
     * fields/conditions.
     */
    private static String signatureOf(MetricRequestDTO req) {
        if (req == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("m=").append(nullToEmpty(req.getMeasurement()));
        sb.append("|r=").append(nullToEmpty(req.getRange()));
        sb.append("|gt=").append(nullToEmpty(req.getGroupTime()));
        sb.append("|lim=").append(req.getLimit() == null ? "" : req.getLimit());

        List<String> groupBy = req.getGroupBy();
        if (groupBy != null && !groupBy.isEmpty()) {
            List<String> sorted = new ArrayList<>(groupBy);
            sorted.sort(Comparator.naturalOrder());
            sb.append("|gb=").append(String.join(",", sorted));
        }

        List<MetricRequestDTO.FieldInfo> fields = req.getFields();
        if (fields != null && !fields.isEmpty()) {
            List<String> rendered = new ArrayList<>(fields.size());
            for (MetricRequestDTO.FieldInfo f : fields) {
                if (f == null) {
                    continue;
                }
                rendered.add(nullToEmpty(f.getFunction()) + ":" + nullToEmpty(f.getField()));
            }
            rendered.sort(Comparator.naturalOrder());
            sb.append("|f=").append(String.join(",", rendered));
        }

        List<MetricRequestDTO.ConditionInfo> conditions = req.getConditions();
        if (conditions != null && !conditions.isEmpty()) {
            List<String> rendered = new ArrayList<>(conditions.size());
            for (MetricRequestDTO.ConditionInfo c : conditions) {
                if (c == null || c.getKey() == null) {
                    continue;
                }
                String key = c.getKey().trim().toLowerCase();
                if (key.equals("ns_id") || key.equals("infra_id") || key.equals("node_id")) {
                    // these are part of the key tuple, not the signature
                    continue;
                }
                rendered.add(key + "=" + nullToEmpty(c.getValue()));
            }
            rendered.sort(Comparator.naturalOrder());
            sb.append("|c=").append(String.join(",", rendered));
        }
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return Objects.toString(s, "");
    }
}
