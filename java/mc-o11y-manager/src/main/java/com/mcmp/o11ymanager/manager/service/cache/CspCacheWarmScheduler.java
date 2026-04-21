package com.mcmp.o11ymanager.manager.service.cache;

import com.mcmp.o11ymanager.manager.config.CspCacheProperties;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterInfo;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterList;
import com.mcmp.o11ymanager.manager.dto.SpiderMonitoringInfo;
import com.mcmp.o11ymanager.manager.dto.influx.VmRef;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.infrastructure.spider.SpiderClient;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
    private final VmCreatedTimeResolver vmCreatedTimeResolver;
    private final InfluxDbService influxDbService;

    private ExecutorService executor;

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
        Set<String> connectionNames = collectConnectionNames(cspVms);

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
        SpiderClusterList list;
        try {
            list = spiderClient.listClusters(connectionName);
        } catch (Exception e) {
            log.debug(
                    "[CSP-CACHE-WARM] listClusters failed conn={}, err={}",
                    connectionName,
                    e.toString());
            return futures;
        }
        if (list == null || list.getCluster() == null) {
            return futures;
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
                    final String nodeNumber = String.valueOf(idx + 1);
                    for (String metric : NODE_METRICS) {
                        futures.add(
                                CompletableFuture.runAsync(
                                        () ->
                                                warmClusterNodeMetric(
                                                        connectionName,
                                                        clusterName,
                                                        ngName,
                                                        nodeNumber,
                                                        metric,
                                                        tbh,
                                                        interval,
                                                        ok,
                                                        fail),
                                        executor));
                    }
                }
            }
        }
        return futures;
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
}
