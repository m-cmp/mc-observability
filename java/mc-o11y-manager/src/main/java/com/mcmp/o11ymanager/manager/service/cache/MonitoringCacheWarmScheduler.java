package com.mcmp.o11ymanager.manager.service.cache;

import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties;
import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties.Job;
import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties.OverviewJob;
import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties.OverviewQuery;
import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties.RangeSpec;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.VmRef;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
 * Periodically pre-warms the monitoring metric cache.
 *
 * <p>Two independent jobs run on separate fixed thread pools:
 *
 * <ul>
 *   <li><b>realtime</b> — short-range queries (e.g. {@code 1h/1m, 6h/5m, 12h/5m}) refreshed every
 *       minute. These hit the raw mc-observability InfluxDB.
 *   <li><b>longrange</b> — long-range queries (e.g. {@code 1d/5m, 3d/15m, 5d/30m, 7d/1h}) refreshed
 *       on the hourly Airflow downsampling DAG cycle. These automatically route to the downsampling
 *       InfluxDB via {@code InfluxDbServiceImpl#pickDatabase}.
 * </ul>
 *
 * For each tick the scheduler discovers active VMs in InfluxDB, sorts by Tumblebug createdTime,
 * keeps the top-N most recently created, and submits per-VM warming tasks (one per measurement ×
 * range combo) to the job's executor.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "monitoring.cache.warm",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class MonitoringCacheWarmScheduler {

    private final MonitoringCacheProperties properties;
    private final InfluxDbService influxDbService;
    private final VmCreatedTimeResolver vmCreatedTimeResolver;

    private ExecutorService realtimeExecutor;
    private ExecutorService longrangeExecutor;
    private ExecutorService overviewExecutor;

    @PostConstruct
    void init() {
        realtimeExecutor =
                newFixedPool("mon-cache-warm-realtime", properties.getWarm().getRealtime());
        longrangeExecutor =
                newFixedPool("mon-cache-warm-longrange", properties.getWarm().getLongrange());
        OverviewJob overview = properties.getWarm().getOverview();
        overviewExecutor =
                newFixedPoolFromSize(
                        "mon-cache-warm-overview",
                        overview == null ? 10 : overview.getThreadPoolSize());
        log.info(
                "[CACHE-WARM] initialized realtimeThreads={}, longrangeThreads={}, overviewThreads={}",
                properties.getWarm().getRealtime().getThreadPoolSize(),
                properties.getWarm().getLongrange().getThreadPoolSize(),
                overview == null ? 0 : overview.getThreadPoolSize());
    }

    @PreDestroy
    void shutdown() {
        if (realtimeExecutor != null) {
            realtimeExecutor.shutdownNow();
        }
        if (longrangeExecutor != null) {
            longrangeExecutor.shutdownNow();
        }
        if (overviewExecutor != null) {
            overviewExecutor.shutdownNow();
        }
    }

    @Scheduled(cron = "${monitoring.cache.warm.realtime.cron:0 * * * * *}")
    public void scheduledRealtime() {
        runJob("realtime", properties.getWarm().getRealtime(), realtimeExecutor);
    }

    @Scheduled(cron = "${monitoring.cache.warm.longrange.cron:0 5 * * * *}")
    public void scheduledLongrange() {
        runJob("longrange", properties.getWarm().getLongrange(), longrangeExecutor);
    }

    @Scheduled(cron = "${monitoring.cache.warm.overview.cron:0 * * * * *}")
    public void scheduledOverview() {
        runOverviewJob();
    }

    /** Triggers all warming jobs immediately (admin endpoint). */
    public int warmNow() {
        int realtime = runJob("realtime", properties.getWarm().getRealtime(), realtimeExecutor);
        int longrange = runJob("longrange", properties.getWarm().getLongrange(), longrangeExecutor);
        int overview = runOverviewJob();
        return realtime + longrange + overview;
    }

    private int runJob(String jobName, Job job, ExecutorService executor) {
        if (job == null
                || executor == null
                || job.getRanges() == null
                || job.getRanges().isEmpty()) {
            return 0;
        }
        long started = System.currentTimeMillis();
        List<VmWithCreatedAt> top = pickTopVms();
        if (top.isEmpty()) {
            log.info("[CACHE-WARM:{}] no eligible VMs", jobName);
            return 0;
        }

        List<String> measurements = discoverMeasurements();
        if (measurements.isEmpty()) {
            log.info("[CACHE-WARM:{}] no measurements discovered", jobName);
            return 0;
        }

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>(top.size());
        for (VmWithCreatedAt entry : top) {
            futures.add(
                    CompletableFuture.runAsync(
                            () ->
                                    warmOneVm(
                                            jobName,
                                            job.getRanges(),
                                            entry.vm(),
                                            measurements,
                                            ok,
                                            fail),
                            executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        log.info(
                "[CACHE-WARM:{}] vms={}, measurements={}, ranges={}, ok={}, fail={}, took={}ms",
                jobName,
                top.size(),
                measurements.size(),
                job.getRanges().size(),
                ok.get(),
                fail.get(),
                System.currentTimeMillis() - started);
        return top.size();
    }

    /** Discover all measurement names known to any configured InfluxDB instance. */
    private List<String> discoverMeasurements() {
        try {
            var body = influxDbService.getFields();
            if (body == null || body.getData() == null) {
                return List.of();
            }
            return body.getData().stream()
                    .map(f -> f.getMeasurement())
                    .filter(m -> m != null && !m.isBlank())
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.warn("[CACHE-WARM] discover measurements failed: {}", e.toString());
            return List.of();
        }
    }

    private void warmOneVm(
            String jobName,
            List<RangeSpec> ranges,
            VmRef vm,
            List<String> measurements,
            AtomicInteger ok,
            AtomicInteger fail) {
        for (String measurement : measurements) {
            for (RangeSpec spec : ranges) {
                try {
                    influxDbService.getMetricsByVM(
                            vm.nsId(), vm.mciId(), vm.vmId(), buildRequest(spec, measurement));
                    ok.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                    log.debug(
                            "[CACHE-WARM:{}] failed ns={}, mci={}, vm={}, m={}, range={}, err={}",
                            jobName,
                            vm.nsId(),
                            vm.mciId(),
                            vm.vmId(),
                            measurement,
                            spec.getRange(),
                            e.toString());
                }
            }
        }
    }

    private int runOverviewJob() {
        OverviewJob job = properties.getWarm().getOverview();
        if (job == null
                || !job.isEnabled()
                || overviewExecutor == null
                || job.getQueries() == null
                || job.getQueries().isEmpty()) {
            return 0;
        }
        long started = System.currentTimeMillis();
        List<VmWithCreatedAt> top = pickTopVms();
        if (top.isEmpty()) {
            log.info("[CACHE-WARM:overview] no eligible VMs");
            return 0;
        }

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>(top.size());
        for (VmWithCreatedAt entry : top) {
            futures.add(
                    CompletableFuture.runAsync(
                            () -> warmOneVmOverview(entry.vm(), job.getQueries(), ok, fail),
                            overviewExecutor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        log.info(
                "[CACHE-WARM:overview] vms={}, queries={}, ok={}, fail={}, took={}ms",
                top.size(),
                job.getQueries().size(),
                ok.get(),
                fail.get(),
                System.currentTimeMillis() - started);
        return top.size();
    }

    private void warmOneVmOverview(
            VmRef vm, List<OverviewQuery> queries, AtomicInteger ok, AtomicInteger fail) {
        for (OverviewQuery q : queries) {
            try {
                influxDbService.getMetricsByVM(
                        vm.nsId(), vm.mciId(), vm.vmId(), buildOverviewRequest(q));
                ok.incrementAndGet();
            } catch (Exception e) {
                fail.incrementAndGet();
                log.debug(
                        "[CACHE-WARM:overview] failed ns={}, mci={}, vm={}, m={}, fn={}, fd={}, err={}",
                        vm.nsId(),
                        vm.mciId(),
                        vm.vmId(),
                        q.getMeasurement(),
                        q.getFunction(),
                        q.getField(),
                        e.toString());
            }
        }
    }

    private MetricRequestDTO buildOverviewRequest(OverviewQuery q) {
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement(q.getMeasurement());
        req.setRange(q.getRange());
        req.setGroupTime(q.getGroupTime());
        req.setLimit(q.getLimit());
        req.setGroupBy(new ArrayList<>(List.of("vm_id")));
        MetricRequestDTO.FieldInfo f = new MetricRequestDTO.FieldInfo();
        f.setFunction(q.getFunction());
        f.setField(q.getField());
        List<MetricRequestDTO.FieldInfo> fields = new ArrayList<>();
        fields.add(f);
        req.setFields(fields);
        req.setConditions(new ArrayList<>());
        return req;
    }

    /** Discover active VMs and return the {@code topN} sorted by createdTime descending. */
    private List<VmWithCreatedAt> pickTopVms() {
        List<VmRef> active;
        try {
            active = influxDbService.discoverActiveVms();
        } catch (Exception e) {
            log.warn("[CACHE-WARM] discover failed: {}", e.toString());
            return List.of();
        }
        if (active.isEmpty()) {
            return List.of();
        }
        List<VmWithCreatedAt> withTime = new ArrayList<>(active.size());
        for (VmRef vm : active) {
            Optional<Instant> createdAt =
                    vmCreatedTimeResolver.resolve(vm.nsId(), vm.mciId(), vm.vmId());
            createdAt.ifPresent(instant -> withTime.add(new VmWithCreatedAt(vm, instant)));
        }
        if (withTime.isEmpty()) {
            return List.of();
        }
        withTime.sort(Comparator.comparing(VmWithCreatedAt::createdAt).reversed());
        int topN = Math.max(1, properties.getWarm().getTopN());
        return new ArrayList<>(withTime.subList(0, Math.min(topN, withTime.size())));
    }

    private MetricRequestDTO buildRequest(RangeSpec spec, String measurement) {
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement(measurement);
        req.setRange(spec.getRange());
        req.setGroupTime(spec.getGroupTime());
        req.setFields(new ArrayList<>());
        req.setConditions(new ArrayList<>());
        return req;
    }

    private static ExecutorService newFixedPool(String namePrefix, Job job) {
        return newFixedPoolFromSize(namePrefix, job.getThreadPoolSize());
    }

    private static ExecutorService newFixedPoolFromSize(String namePrefix, int size) {
        int bounded = Math.max(1, size);
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory =
                r -> {
                    Thread t = new Thread(r, namePrefix + "-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                };
        return Executors.newFixedThreadPool(bounded, factory);
    }

    private record VmWithCreatedAt(VmRef vm, Instant createdAt) {}
}
