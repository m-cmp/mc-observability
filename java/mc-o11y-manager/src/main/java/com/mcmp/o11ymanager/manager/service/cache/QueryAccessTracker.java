package com.mcmp.o11ymanager.manager.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mcmp.o11ymanager.manager.dto.influx.VmRef;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Records which VMs are actually being queried, so cache warming can target them.
 *
 * <p>Warming used to pick the most recently created VMs. Creation order is a poor proxy for demand
 * — the VMs worth pre-loading are the ones an operator currently has open on a dashboard, which may
 * have been created months ago. This keeps a bounded, time-limited view of real query traffic and
 * ranks by hit count, breaking ties on recency.
 */
@Component
public class QueryAccessTracker {

    private final Cache<String, Entry> recent;

    public QueryAccessTracker(
            @Value("${monitoring.cache.access-tracker.max-size:2000}") long maxSize,
            @Value("${monitoring.cache.access-tracker.window-seconds:900}") long windowSeconds) {
        this.recent =
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(Duration.ofSeconds(Math.max(60, windowSeconds)))
                        .build();
    }

    /** Notes one query against a VM-scoped target. ns/mci-scoped queries are ignored. */
    public void record(String nsId, String infraId, String nodeId) {
        if (nsId == null || infraId == null || nodeId == null || nodeId.isEmpty()) {
            return;
        }
        String key = nsId + "/" + infraId + "/" + nodeId;
        Entry e = recent.get(key, k -> new Entry(new VmRef(nsId, infraId, nodeId)));
        if (e != null) {
            e.hits.incrementAndGet();
            e.lastSeenMillis.set(System.currentTimeMillis());
        }
    }

    /** Most-queried VMs within the tracking window, most demanded first. */
    public List<VmRef> topVms(int limit) {
        Map<String, Entry> snapshot = recent.asMap();
        if (snapshot.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>(snapshot.values());
        entries.sort(
                Comparator.comparingLong((Entry e) -> e.hits.get())
                        .reversed()
                        .thenComparing(
                                Comparator.comparingLong((Entry e) -> e.lastSeenMillis.get())
                                        .reversed()));
        List<VmRef> out = new ArrayList<>(Math.min(limit, entries.size()));
        for (int i = 0; i < entries.size() && out.size() < limit; i++) {
            out.add(entries.get(i).vm);
        }
        return out;
    }

    /** Number of distinct VMs currently tracked. */
    public long size() {
        return recent.estimatedSize();
    }

    private static final class Entry {
        private final VmRef vm;
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong lastSeenMillis = new AtomicLong(System.currentTimeMillis());

        private Entry(VmRef vm) {
            this.vm = vm;
        }
    }
}
