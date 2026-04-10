package com.mcmp.o11ymanager.manager.service.cache;

import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Cache key for monitoring metric query results.
 *
 * <p>The {@code hourBucket} component ensures that queries fall into 1-hour wall-clock blocks: two
 * identical requests in the same hour return cached data, while a new hour produces a fresh entry.
 */
public record MonitoringCacheKey(
        String nsId, String mciId, String vmId, String requestSignature, long hourBucket) {

    /** Builds the canonical cache key for an ns/mci-scoped metric query. */
    public static MonitoringCacheKey of(
            String nsId, String mciId, String vmId, MetricRequestDTO req, long blockPeriodSeconds) {
        long bucket = (System.currentTimeMillis() / 1000L) / blockPeriodSeconds;
        return new MonitoringCacheKey(
                nullToEmpty(nsId), nullToEmpty(mciId), nullToEmpty(vmId), signatureOf(req), bucket);
    }

    /**
     * Stable signature for a {@link MetricRequestDTO} that ignores ns_id/mci_id/vm_id conditions
     * (those live in their own key fields) and is order-insensitive for fields/conditions.
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
                if (key.equals("ns_id") || key.equals("mci_id") || key.equals("vm_id")) {
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
