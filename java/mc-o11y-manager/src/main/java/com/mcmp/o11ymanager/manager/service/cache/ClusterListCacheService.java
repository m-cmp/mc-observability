package com.mcmp.o11ymanager.manager.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterInfo;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterList;
import com.mcmp.o11ymanager.manager.infrastructure.spider.SpiderClient;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Caches the (expensive) per-connection K8s cluster discovery + node-group detail (cb-spider {@code
 * listClusters} + {@code getCluster} per cluster). Shared between the cached controller endpoint
 * and the background warmer so a cold UI load never pays the multi-second cb-spider cost.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterListCacheService {

    private final SpiderClient spiderClient;

    /**
     * TTL must comfortably exceed one warm pass so entries don't expire mid-cycle and force the UI
     * onto the slow cb-spider path. A warm pass can take minutes when some CSP connections are slow
     * to list, so keep this generous — the warmer refreshes entries well before they expire.
     */
    @Value("${csp.cache.cluster-list.ttl-seconds:600}")
    private long ttlSeconds;

    private Cache<String, List<SpiderClusterInfo>> cache;

    @PostConstruct
    void init() {
        cache =
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                        .maximumSize(200)
                        .build();
        log.info("[CLUSTER-LIST-CACHE] enabled ttlSec={}", ttlSeconds);
    }

    /** Cached read (loads on miss). */
    public List<SpiderClusterInfo> get(String connectionName) {
        return cache.get(connectionName, this::load);
    }

    /** Force-refresh the cache entry (used by the background warmer). */
    public void warm(String connectionName) {
        try {
            cache.put(connectionName, load(connectionName));
        } catch (Exception e) {
            log.warn("[CLUSTER-LIST-WARM] conn={} failed: {}", connectionName, e.toString());
        }
    }

    private List<SpiderClusterInfo> load(String connectionName) {
        List<SpiderClusterInfo> out = new ArrayList<>();
        try {
            SpiderClusterList list = spiderClient.listClusters(connectionName);
            if (list != null && list.getCluster() != null) {
                for (SpiderClusterInfo c : list.getCluster()) {
                    try {
                        out.add(spiderClient.getCluster(c.getIId().getNameId(), connectionName));
                    } catch (Exception e) {
                        out.add(c);
                    }
                }
            }
        } catch (Exception e) {
            // cb-spider is slow/erroring for this connection (read timeout, 5xx, stale clusters).
            // Cache an empty result so the UI returns instantly instead of repeatedly paying the
            // slow cb-spider cost; the warmer keeps retrying and fills in real data once it
            // recovers.
            log.warn(
                    "[CLUSTER-LIST] conn={} list failed, caching empty: {}",
                    connectionName,
                    e.toString());
        }
        return out;
    }
}
