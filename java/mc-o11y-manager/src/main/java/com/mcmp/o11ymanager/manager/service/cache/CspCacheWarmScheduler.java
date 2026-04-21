package com.mcmp.o11ymanager.manager.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mcmp.o11ymanager.manager.config.CspCacheProperties;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterInfo;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterList;
import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;
import com.mcmp.o11ymanager.manager.dto.influx.VmRef;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCIList;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.manager.infrastructure.spider.SpiderClient;
import com.mcmp.o11ymanager.manager.infrastructure.tumblebug.TumblebugClient;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pre-warms the CSP monitoring cache so the NS/MCI overview doesn't pay cold-cache latency on tab
 * switch.
 *
 * <p>Each tick: discover active VMs via InfluxDB, resolve their CSP identifiers via Tumblebug, then
 * fetch all {@link #VM_METRICS} in parallel. Clusters and cluster nodes are discovered per unique
 * connection name and warmed with {@link #NODE_METRICS}.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "csp.cache.warm",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class CspCacheWarmScheduler {

    /** CSP metrics kept in sync with the frontend's CSP_METRICS list in {@code api/csp.js}. */
    static final List<String> VM_METRICS =
            List.of(
                    "cpu_usage",
                    "memory_usage",
                    "disk_read",
                    "disk_write",
                    "disk_read_ops",
                    "disk_write_ops",
                    "network_in",
                    "network_out");

    /** K8s node metrics — same shape as VM_METRICS (cb-spider uses identical metric keys). */
    static final List<String> NODE_METRICS = VM_METRICS;

    private static final List<String> CSP_SUPPORTED_PROVIDERS = List.of("aws", "azure", "gcp");

    private final CspCacheProperties properties;
    private final CspCacheService cspCacheService;
    private final SpiderClient spiderClient;
    private final TumblebugService tumblebugService;
    private final TumblebugClient tumblebugClient;
    private final VmCreatedTimeResolver vmCreatedTimeResolver;
    private final InfluxDbService influxDbService;

    private ExecutorService executor;

    /**
     * Short-lived caches for Tumblebug/cb-spider discovery calls so the warm pass doesn't hammer
     * Tumblebug's rate limiter on every tick (it returns 429 quickly under bursts).
     *
     * <p>TTL roughly covers 5 warm cycles — fresh enough to pick up new infrastructure within a few
     * minutes, cheap enough to not re-discover on every minute.
     */
    private final Cache<String, Set<String>> connectionDiscoveryCache =
            Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofMinutes(5)).build();

    private final Cache<String, List<ClusterRef>> clusterDiscoveryCache =
            Caffeine.newBuilder().maximumSize(64).expireAfterWrite(Duration.ofMinutes(5)).build();

    @PostConstruct
    void init() {
        int size = Math.max(1, properties.getWarm().getThreadPoolSize());
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory =
                r -> {
                    Thread t = new Thread(r, "csp-cache-warm-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                };
        this.executor = Executors.newFixedThreadPool(size, factory);
        log.info(
                "[CSP-CACHE-WARM] initialized threads={}, topN={}, tbh={}, interval={}",
                size,
                properties.getWarm().getTopN(),
                properties.getWarm().getTimeBeforeHour(),
                properties.getWarm().getIntervalMinute());
    }

    @PreDestroy
    void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Scheduled(cron = "${csp.cache.warm.cron:0 * * * * *}")
    public void scheduled() {
        warmNow();
    }

    public int warmNow() {
        if (executor == null || !properties.isEnabled()) {
            return 0;
        }
        long started = System.currentTimeMillis();
        CspCacheProperties.Warm w = properties.getWarm();

        List<VmWithCsp> cspVms = collectCspVms();
        // Connection names for cluster discovery come from ALL Tumblebug VMs in all NSs,
        // not just InfluxDB-active VMs. K8s nodes don't report agent metrics so their
        // connections would otherwise be missed.
        Set<String> connectionNames = collectAllTumblebugConnections();

        AtomicInteger vmOk = new AtomicInteger();
        AtomicInteger vmFail = new AtomicInteger();
        AtomicInteger nodeOk = new AtomicInteger();
        AtomicInteger nodeFail = new AtomicInteger();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (VmWithCsp v : cspVms) {
            for (String metric : VM_METRICS) {
                futures.add(
                        CompletableFuture.runAsync(
                                () ->
                                        warmVmMetric(
                                                v,
                                                metric,
                                                w.getTimeBeforeHour(),
                                                w.getIntervalMinute(),
                                                vmOk,
                                                vmFail),
                                executor));
            }
        }
        for (String conn : connectionNames) {
            futures.addAll(
                    warmClustersForConnection(
                            conn, w.getTimeBeforeHour(), w.getIntervalMinute(), nodeOk, nodeFail));
        }

        if (futures.isEmpty()) {
            log.info("[CSP-CACHE-WARM] no CSP VMs or clusters to warm");
            return 0;
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        log.info(
                "[CSP-CACHE-WARM] vms={}, vmOk={}, vmFail={}, connections={}, nodeOk={}, nodeFail={}, took={}ms",
                cspVms.size(),
                vmOk.get(),
                vmFail.get(),
                connectionNames.size(),
                nodeOk.get(),
                nodeFail.get(),
                System.currentTimeMillis() - started);
        return cspVms.size();
    }

    private List<VmWithCsp> collectCspVms() {
        List<VmRef> active;
        try {
            active = influxDbService.discoverActiveVms();
        } catch (Exception e) {
            log.warn("[CSP-CACHE-WARM] discoverActiveVms failed: {}", e.toString());
            return List.of();
        }
        if (active.isEmpty()) {
            return List.of();
        }

        List<VmWithCsp> resolved = new ArrayList<>(active.size());
        for (VmRef ref : active) {
            try {
                TumblebugMCI.Vm vm = tumblebugService.getVm(ref.nsId(), ref.mciId(), ref.vmId());
                if (vm == null
                        || vm.getConnectionName() == null
                        || vm.getCspResourceName() == null) {
                    continue;
                }
                if (!isCspSupported(vm.getConnectionName())) {
                    continue;
                }
                Instant createdAt =
                        vmCreatedTimeResolver
                                .resolve(ref.nsId(), ref.mciId(), ref.vmId())
                                .orElse(Instant.EPOCH);
                resolved.add(
                        new VmWithCsp(
                                ref, vm.getConnectionName(), vm.getCspResourceName(), createdAt));
            } catch (Exception e) {
                log.debug(
                        "[CSP-CACHE-WARM] resolve failed ns={}, mci={}, vm={}, err={}",
                        ref.nsId(),
                        ref.mciId(),
                        ref.vmId(),
                        e.toString());
            }
        }
        resolved.sort(Comparator.comparing(VmWithCsp::createdAt).reversed());
        int topN = Math.max(1, properties.getWarm().getTopN());
        return new ArrayList<>(resolved.subList(0, Math.min(topN, resolved.size())));
    }

    private Set<String> collectConnectionNames(List<VmWithCsp> vms) {
        Set<String> out = new HashSet<>();
        for (VmWithCsp v : vms) {
            if (v.connectionName() != null && !v.connectionName().isBlank()) {
                out.add(v.connectionName());
            }
        }
        return out;
    }

    /**
     * Walks every namespace and MCI in Tumblebug and collects unique connectionNames. Result is
     * memoised for 5 minutes to avoid Tumblebug rate-limit bursts (429 Too Many Requests) when warm
     * runs every minute.
     */
    private Set<String> collectAllTumblebugConnections() {
        Set<String> cached = connectionDiscoveryCache.getIfPresent("all");
        if (cached != null) {
            return cached;
        }
        Set<String> out = new HashSet<>();
        int nsSeen = 0;
        int mciSeen = 0;
        int vmSeen = 0;
        int vmFiltered = 0;
        TumblebugNS nsList;
        try {
            nsList = tumblebugClient.getNSList();
        } catch (Exception e) {
            log.warn("[CSP-CACHE-WARM] getNSList failed: {}", e.toString());
            connectionDiscoveryCache.put("all", out);
            return out;
        }
        if (nsList == null || nsList.getNs() == null) {
            log.info("[CSP-CACHE-WARM] getNSList returned null/empty");
            connectionDiscoveryCache.put("all", out);
            return out;
        }
        for (TumblebugNS.NS ns : nsList.getNs()) {
            if (ns == null || ns.getId() == null) {
                continue;
            }
            nsSeen++;
            TumblebugMCIList mciList;
            try {
                mciList = tumblebugClient.getMCIList(ns.getId());
            } catch (Exception e) {
                log.warn(
                        "[CSP-CACHE-WARM] getMCIList failed ns={}, err={}",
                        ns.getId(),
                        e.toString());
                continue;
            }
            if (mciList == null || mciList.getMci() == null) {
                continue;
            }
            for (TumblebugMCI mci : mciList.getMci()) {
                mciSeen++;
                if (mci == null || mci.getVm() == null) {
                    continue;
                }
                for (TumblebugMCI.Vm vm : mci.getVm()) {
                    vmSeen++;
                    if (vm != null
                            && vm.getConnectionName() != null
                            && !vm.getConnectionName().isBlank()
                            && isCspSupported(vm.getConnectionName())) {
                        out.add(vm.getConnectionName());
                    } else {
                        vmFiltered++;
                    }
                }
            }
        }
        log.info(
                "[CSP-CACHE-WARM] discovery: ns={}, mci={}, vm={}, filtered={}, connections={}",
                nsSeen,
                mciSeen,
                vmSeen,
                vmFiltered,
                out);
        connectionDiscoveryCache.put("all", out);
        return out;
    }

    private void warmVmMetric(
            VmWithCsp v,
            String metric,
            String tbh,
            String interval,
            AtomicInteger ok,
            AtomicInteger fail) {
        try {
            SpiderMonitoringInfo.Data data =
                    spiderClient.getVMMonitoring(
                            v.cspResourceName(), metric, v.connectionName(), tbh, interval);
            cspCacheService.put(
                    CspCacheKey.forVm(
                            v.cspResourceName(), metric, v.connectionName(), tbh, interval),
                    data);
            ok.incrementAndGet();
        } catch (Exception e) {
            fail.incrementAndGet();
            log.debug(
                    "[CSP-CACHE-WARM] VM fetch failed vm={}, metric={}, conn={}, err={}",
                    v.cspResourceName(),
                    metric,
                    v.connectionName(),
                    e.toString());
        }
    }

    private List<CompletableFuture<Void>> warmClustersForConnection(
            String connectionName,
            String tbh,
            String interval,
            AtomicInteger ok,
            AtomicInteger fail) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<ClusterRef> refs = discoverClusters(connectionName);
        for (ClusterRef ref : refs) {
            for (String metric : NODE_METRICS) {
                futures.add(
                        CompletableFuture.runAsync(
                                () ->
                                        warmClusterNodeMetric(
                                                ref.connectionName(),
                                                ref.clusterName(),
                                                ref.nodeGroupName(),
                                                ref.nodeNumber(),
                                                metric,
                                                tbh,
                                                interval,
                                                ok,
                                                fail),
                                executor));
            }
        }
        return futures;
    }

    /**
     * Enumerates (cluster, nodeGroup, nodeNumber) tuples for a connection. Result is cached for 5
     * minutes to avoid hammering cb-spider (which itself talks to the CSP API) each warm tick.
     */
    private List<ClusterRef> discoverClusters(String connectionName) {
        List<ClusterRef> cached = clusterDiscoveryCache.getIfPresent(connectionName);
        if (cached != null) {
            return cached;
        }
        List<ClusterRef> refs = new ArrayList<>();
        SpiderClusterList list;
        try {
            list = spiderClient.listClusters(connectionName);
        } catch (Exception e) {
            log.debug(
                    "[CSP-CACHE-WARM] listClusters failed conn={}, err={}",
                    connectionName,
                    e.toString());
            return refs;
        }
        if (list == null || list.getCluster() == null) {
            return refs;
        }
        for (SpiderClusterInfo c : list.getCluster()) {
            if (c == null || c.getIId() == null || c.getIId().getNameId() == null) {
                continue;
            }
            SpiderClusterInfo detail;
            try {
                detail = spiderClient.getCluster(c.getIId().getNameId(), connectionName);
            } catch (Exception e) {
                log.debug(
                        "[CSP-CACHE-WARM] getCluster failed cluster={}, err={}",
                        c.getIId().getNameId(),
                        e.toString());
                continue;
            }
            if (detail == null || detail.getNodeGroupList() == null) {
                continue;
            }
            String clusterName = detail.getIId() != null ? detail.getIId().getNameId() : null;
            if (clusterName == null) {
                continue;
            }
            for (SpiderClusterInfo.NodeGroup ng : detail.getNodeGroupList()) {
                if (ng == null
                        || ng.getIId() == null
                        || ng.getIId().getNameId() == null
                        || ng.getNodes() == null) {
                    continue;
                }
                String ngName = ng.getIId().getNameId();
                for (int idx = 0; idx < ng.getNodes().size(); idx++) {
                    refs.add(
                            new ClusterRef(
                                    connectionName, clusterName, ngName, String.valueOf(idx + 1)));
                }
            }
        }
        // See collectAllTumblebugConnections — cache even when empty to avoid re-hitting
        // cb-spider/CSP on every warm tick.
        clusterDiscoveryCache.put(connectionName, refs);
        return refs;
    }

    private void warmClusterNodeMetric(
            String connectionName,
            String clusterName,
            String nodeGroupName,
            String nodeNumber,
            String metric,
            String tbh,
            String interval,
            AtomicInteger ok,
            AtomicInteger fail) {
        try {
            SpiderMonitoringInfo.Data data =
                    spiderClient.getClusterNodeMonitoring(
                            clusterName,
                            nodeGroupName,
                            nodeNumber,
                            metric,
                            connectionName,
                            tbh,
                            interval);
            cspCacheService.put(
                    CspCacheKey.forClusterNode(
                            clusterName,
                            nodeGroupName,
                            nodeNumber,
                            metric,
                            connectionName,
                            tbh,
                            interval),
                    data);
            ok.incrementAndGet();
        } catch (Exception e) {
            fail.incrementAndGet();
            log.debug(
                    "[CSP-CACHE-WARM] node fetch failed cluster={}, ng={}, n={}, metric={}, err={}",
                    clusterName,
                    nodeGroupName,
                    nodeNumber,
                    metric,
                    e.toString());
        }
    }

    private static boolean isCspSupported(String connectionName) {
        if (connectionName == null) {
            return false;
        }
        String lower = connectionName.toLowerCase();
        for (String p : CSP_SUPPORTED_PROVIDERS) {
            if (lower.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private record VmWithCsp(
            VmRef ref, String connectionName, String cspResourceName, Instant createdAt) {}

    private record ClusterRef(
            String connectionName, String clusterName, String nodeGroupName, String nodeNumber) {}
}
