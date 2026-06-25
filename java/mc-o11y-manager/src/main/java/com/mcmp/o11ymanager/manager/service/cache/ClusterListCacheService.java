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

    /** TTL must exceed the warmer interval so warmed entries never expire between ticks. */
    @Value("${csp.cache.cluster-list.ttl-seconds:120}")
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
        return out;
    }
}
