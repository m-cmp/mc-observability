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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically pre-warms the monitoring metric cache.
 *
 * <p>Three jobs run on their own fixed thread pools: {@code realtime} for short ranges, {@code
 * longrange} for ranges that route to the downsampling database, and {@code overview} for the exact
 * query shape the NS/MCI overview screen sends.
 *
 * <p>A few properties are worth calling out, because the previous implementation got them wrong:
 *
 * <ul>
 *   <li><b>Warming forces a reload.</b> It goes through {@code refreshNow}, not the normal read
 *       path. Reading through the cache meant that after the first tick filled an entry every later
 *       tick was a no-op and the data simply froze until the key rotated.
 *   <li><b>A pass never blocks the scheduler thread.</b> Spring's scheduler is single-threaded by
 *       default and several jobs fire on the same boundary, so a pass that waits for completion
 *       delays every other job. Passes run detached under a timebox.
 *   <li><b>Targets come from real query traffic.</b> Creation order is a poor proxy for what people
 *       are actually looking at.
 *   <li><b>Only measurements a VM really reports are warmed.</b> The old cartesian product over
 *       every known measurement generated mostly-empty queries.
 * </ul>
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
    private final QueryAccessTracker accessTracker;

    private ExecutorService realtimeExecutor;
    private ExecutorService longrangeExecutor;
    private ExecutorService overviewExecutor;

    /** Guards against a pass starting while the previous one is still running. */
    private final AtomicBoolean realtimeRunning = new AtomicBoolean();

    private final AtomicBoolean longrangeRunning = new AtomicBoolean();
    private final AtomicBoolean overviewRunning = new AtomicBoolean();

    private final AtomicLong passesRun = new AtomicLong();
    private final AtomicLong passesSkippedOverlap = new AtomicLong();
    private final AtomicLong passesTimedOut = new AtomicLong();

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
                "[CACHE-WARM] initialized selection={}, topN={}, timeboxSec={}, jitterSec={},"
                        + " realtimeThreads={}, longrangeThreads={}, overviewThreads={}",
                properties.getWarm().getSelection(),
                properties.getWarm().getTopN(),
                properties.getWarm().getTimeboxSeconds(),
                properties.getWarm().getJitterSeconds(),
                properties.getWarm().getRealtime().getThreadPoolSize(),
                properties.getWarm().getLongrange().getThreadPoolSize(),
                overview == null ? 0 : overview.getThreadPoolSize());
    }

    @PreDestroy
    void shutdown() {
        shutdownPool(realtimeExecutor);
        shutdownPool(longrangeExecutor);
        shutdownPool(overviewExecutor);
    }

    @Scheduled(cron = "${monitoring.cache.warm.realtime.cron:0 * * * * *}")
    public void scheduledRealtime() {
        launch(
                "realtime",
                realtimeRunning,
                () -> runJob("realtime", properties.getWarm().getRealtime(), realtimeExecutor));
    }

    @Scheduled(cron = "${monitoring.cache.warm.longrange.cron:0 5 * * * *}")
    public void scheduledLongrange() {
        launch(
                "longrange",
                longrangeRunning,
                () -> runJob("longrange", properties.getWarm().getLongrange(), longrangeExecutor));
    }

    @Scheduled(cron = "${monitoring.cache.warm.overview.cron:0 * * * * *}")
    public void scheduledOverview() {
        launch("overview", overviewRunning, this::runOverviewJob);
    }

    /** Triggers all warming jobs immediately and waits for them (admin endpoint). */
    public int warmNow() {
        int realtime = runJob("realtime", properties.getWarm().getRealtime(), realtimeExecutor);
        int longrange = runJob("longrange", properties.getWarm().getLongrange(), longrangeExecutor);
        int overview = runOverviewJob();
        return realtime + longrange + overview;
    }

    /** Warming pass counters, folded into the cache stats endpoint. */
    public java.util.Map<String, Object> stats() {
        return java.util.Map.of(
                "passesRun", passesRun.get(),
                "passesSkippedOverlap", passesSkippedOverlap.get(),
                "passesTimedOut", passesTimedOut.get(),
                "selection", properties.getWarm().getSelection(),
                "topN", properties.getWarm().getTopN());
    }

    /**
     * Runs a pass off the scheduler thread, applying jitter and a timebox, and refusing to start
     * when the previous pass of the same job has not finished.
     */
    private void launch(String jobName, AtomicBoolean guard, Runnable body) {
        if (!guard.compareAndSet(false, true)) {
            passesSkippedOverlap.incrementAndGet();
            log.info("[CACHE-WARM:{}] previous pass still running — skipping this tick", jobName);
            return;
        }
        long jitterSec = Math.max(0L, properties.getWarm().getJitterSeconds());
        long delayMillis =
                jitterSec == 0 ? 0 : ThreadLocalRandom.current().nextLong(jitterSec * 1000L);
        long timeboxSec = Math.max(5L, properties.getWarm().getTimeboxSeconds());

        CompletableFuture<Void> pass =
                CompletableFuture.runAsync(
                        () -> {
                            try {
                                if (delayMillis > 0) {
                                    Thread.sleep(delayMillis);
                                }
                                passesRun.incrementAndGet();
                                body.run();
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            } catch (Exception e) {
                                log.warn("[CACHE-WARM:{}] pass failed: {}", jobName, e.toString());
                            }
                        });
        pass.orTimeout(timeboxSec + jitterSec, TimeUnit.SECONDS)
                .whenComplete(
                        (ignored, error) -> {
                            guard.set(false);
                            if (error instanceof TimeoutException) {
                                passesTimedOut.incrementAndGet();
                                log.warn(
                                        "[CACHE-WARM:{}] pass exceeded timebox of {}s — abandoned",
                                        jobName,
                                        timeboxSec);
                            }
                        });
    }

    private int runJob(String jobName, Job job, ExecutorService executor) {
        if (job == null
                || executor == null
                || job.getRanges() == null
                || job.getRanges().isEmpty()) {
            return 0;
        }
        long started = System.currentTimeMillis();
        List<VmRef> targets = pickTargets();
        if (targets.isEmpty()) {
            log.info("[CACHE-WARM:{}] no eligible VMs", jobName);
            return 0;
        }

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>(targets.size());
        for (VmRef vm : targets) {
            futures.add(
                    CompletableFuture.runAsync(
                            () -> warmOneVm(jobName, job.getRanges(), vm, ok, fail, skipped),
                            executor));
        }
        awaitWithin(futures, properties.getWarm().getTimeboxSeconds(), jobName);

        log.info(
                "[CACHE-WARM:{}] vms={}, ranges={}, ok={}, skipped={}, fail={}, took={}ms",
                jobName,
                targets.size(),
                job.getRanges().size(),
                ok.get(),
                skipped.get(),
                fail.get(),
                System.currentTimeMillis() - started);
        return targets.size();
    }

    private void warmOneVm(
            String jobName,
            List<RangeSpec> ranges,
            VmRef vm,
            AtomicInteger ok,
            AtomicInteger fail,
            AtomicInteger skipped) {
        List<String> measurements = measurementsFor(vm);
        if (measurements.isEmpty()) {
            return;
        }
        for (String measurement : measurements) {
            for (RangeSpec spec : ranges) {
                MetricRequestDTO req = buildRequest(spec, measurement);
                try {
                    // Skip when the entry is still inside its fresh window — no point reloading
                    // data the cache would have served anyway.
                    if (influxDbService.isMetricCacheFresh(
                            vm.nsId(), vm.infraId(), vm.nodeId(), req)) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    influxDbService.refreshMetricsByVM(vm.nsId(), vm.infraId(), vm.nodeId(), req);
                    ok.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                    log.debug(
                            "[CACHE-WARM:{}] failed ns={}, mci={}, vm={}, m={}, range={}, err={}",
                            jobName,
                            vm.nsId(),
                            vm.infraId(),
                            vm.nodeId(),
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
        List<VmRef> targets = pickTargets();
        if (targets.isEmpty()) {
            log.info("[CACHE-WARM:overview] no eligible VMs");
            return 0;
        }

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>(targets.size());
        for (VmRef vm : targets) {
            futures.add(
                    CompletableFuture.runAsync(
                            () -> warmOneVmOverview(vm, job.getQueries(), ok, fail),
                            overviewExecutor));
        }
        awaitWithin(futures, properties.getWarm().getTimeboxSeconds(), "overview");

        log.info(
                "[CACHE-WARM:overview] vms={}, queries={}, ok={}, fail={}, took={}ms",
                targets.size(),
                job.getQueries().size(),
                ok.get(),
                fail.get(),
                System.currentTimeMillis() - started);
        return targets.size();
    }

    private void warmOneVmOverview(
            VmRef vm, List<OverviewQuery> queries, AtomicInteger ok, AtomicInteger fail) {
        for (OverviewQuery q : queries) {
            try {
                influxDbService.refreshMetricsByVM(
                        vm.nsId(), vm.infraId(), vm.nodeId(), buildOverviewRequest(q));
                ok.incrementAndGet();
            } catch (Exception e) {
                fail.incrementAndGet();
                log.debug(
                        "[CACHE-WARM:overview] failed ns={}, mci={}, vm={}, m={}, err={}",
                        vm.nsId(),
                        vm.infraId(),
                        vm.nodeId(),
                        q.getMeasurement(),
                        e.toString());
            }
        }
    }

    /** Measurements this VM actually reports, resolved once and memoised by the meta cache. */
    private List<String> measurementsFor(VmRef vm) {
        try {
            return influxDbService.measurementsOfVm(vm.nsId(), vm.infraId(), vm.nodeId());
        } catch (Exception e) {
            log.debug(
                    "[CACHE-WARM] measurement lookup failed ns={}, mci={}, vm={}, err={}",
                    vm.nsId(),
                    vm.infraId(),
                    vm.nodeId(),
                    e.toString());
            return List.of();
        }
    }

    /**
     * Warming targets: the VMs users are actually querying, topped up with recently created ones
     * when traffic alone does not fill the quota (which is the case right after a restart).
     */
    private List<VmRef> pickTargets() {
        int topN = Math.max(1, properties.getWarm().getTopN());
        boolean preferQueried =
                !"recently-created".equalsIgnoreCase(properties.getWarm().getSelection());

        Set<VmRef> picked = new LinkedHashSet<>(topN * 2);
        if (preferQueried) {
            picked.addAll(accessTracker.topVms(topN));
        }
        if (picked.size() < topN) {
            picked.addAll(recentlyCreatedVms(topN - picked.size()));
        }
        List<VmRef> out = new ArrayList<>(picked);
        return out.size() > topN ? out.subList(0, topN) : out;
    }

    /** Discover active VMs and return the most recently created ones. */
    private List<VmRef> recentlyCreatedVms(int limit) {
        if (limit <= 0) {
            return List.of();
        }
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
                    vmCreatedTimeResolver.resolve(vm.nsId(), vm.infraId(), vm.nodeId());
            createdAt.ifPresent(instant -> withTime.add(new VmWithCreatedAt(vm, instant)));
        }
        if (withTime.isEmpty()) {
            // Tumblebug may not expose createdTime at all — fall back to discovery order rather
            // than warming nothing.
            return active.size() > limit ? active.subList(0, limit) : active;
        }
        withTime.sort(Comparator.comparing(VmWithCreatedAt::createdAt).reversed());
        List<VmRef> out = new ArrayList<>(Math.min(limit, withTime.size()));
        for (int i = 0; i < withTime.size() && out.size() < limit; i++) {
            out.add(withTime.get(i).vm());
        }
        return out;
    }

    /** Waits for the pass, giving up once the timebox elapses instead of blocking indefinitely. */
    private void awaitWithin(
            List<CompletableFuture<Void>> futures, long timeboxSeconds, String jobName) {
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .get(Math.max(5L, timeboxSeconds), TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            passesTimedOut.incrementAndGet();
            futures.forEach(f -> f.cancel(true));
            log.warn("[CACHE-WARM:{}] timebox elapsed — remaining work cancelled", jobName);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("[CACHE-WARM:{}] await failed: {}", jobName, e.toString());
        }
    }

    private MetricRequestDTO buildOverviewRequest(OverviewQuery q) {
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement(q.getMeasurement());
        req.setRange(q.getRange());
        req.setGroupTime(q.getGroupTime());
        req.setLimit(q.getLimit());
        req.setGroupBy(new ArrayList<>(List.of("node_id")));
        MetricRequestDTO.FieldInfo f = new MetricRequestDTO.FieldInfo();
        f.setFunction(q.getFunction());
        f.setField(q.getField());
        List<MetricRequestDTO.FieldInfo> fields = new ArrayList<>();
        fields.add(f);
        req.setFields(fields);
        req.setConditions(new ArrayList<>());
        return req;
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

    private static void shutdownPool(ExecutorService pool) {
        if (pool != null) {
            pool.shutdownNow();
        }
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
