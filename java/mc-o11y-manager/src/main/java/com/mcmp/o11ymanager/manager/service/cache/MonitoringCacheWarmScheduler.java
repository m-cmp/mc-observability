package com.mcmp.o11ymanager.manager.service.cache;

import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties;
import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties.Job;
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
 *   <li><b>realtime</b> — short-range queries (e.g. {@code 1h / 1m}) refreshed every minute.
 *   <li><b>downsampling</b> — long-range queries (e.g. {@code 7d / 1h}) refreshed on the hourly
 *       Airflow downsampling DAG cycle.
 * </ul>
 *
 * For each tick the scheduler discovers active VMs in InfluxDB, sorts by Tumblebug createdTime,
 * keeps the top-N most recently created, and submits per-VM warming tasks to the job's executor.
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
    private ExecutorService downsamplingExecutor;

    @PostConstruct
    void init() {
        realtimeExecutor =
                newFixedPool("mon-cache-warm-realtime", properties.getWarm().getRealtime());
        downsamplingExecutor =
                newFixedPool("mon-cache-warm-down", properties.getWarm().getDownsampling());
        log.info(
                "[CACHE-WARM] initialized realtimeThreads={}, downsamplingThreads={}",
                properties.getWarm().getRealtime().getThreadPoolSize(),
                properties.getWarm().getDownsampling().getThreadPoolSize());
    }

    @PreDestroy
    void shutdown() {
        if (realtimeExecutor != null) {
            realtimeExecutor.shutdownNow();
        }
        if (downsamplingExecutor != null) {
            downsamplingExecutor.shutdownNow();
        }
    }

    @Scheduled(cron = "${monitoring.cache.warm.realtime.cron:0 * * * * *}")
    public void scheduledRealtime() {
        runJob("realtime", properties.getWarm().getRealtime(), realtimeExecutor);
    }

    @Scheduled(cron = "${monitoring.cache.warm.downsampling.cron:0 5 * * * *}")
    public void scheduledDownsampling() {
        runJob("downsampling", properties.getWarm().getDownsampling(), downsamplingExecutor);
    }

    /** Triggers both warming jobs immediately (admin endpoint). */
    public int warmNow() {
        int realtime = runJob("realtime", properties.getWarm().getRealtime(), realtimeExecutor);
        int downsampling =
                runJob(
                        "downsampling",
                        properties.getWarm().getDownsampling(),
                        downsamplingExecutor);
        return realtime + downsampling;
    }

    private int runJob(String jobName, Job job, ExecutorService executor) {
        if (job == null || executor == null) {
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
                            () -> warmOneVm(jobName, job, entry.vm(), measurements, ok, fail),
                            executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        log.info(
                "[CACHE-WARM:{}] vms={}, measurements={}, ok={}, fail={}, took={}ms",
                jobName,
                top.size(),
                measurements.size(),
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
            Job job,
            VmRef vm,
            List<String> measurements,
            AtomicInteger ok,
            AtomicInteger fail) {
        for (String measurement : measurements) {
            try {
                influxDbService.getMetricsByVM(
                        vm.nsId(), vm.mciId(), vm.vmId(), buildRequest(job, measurement));
                ok.incrementAndGet();
            } catch (Exception e) {
                fail.incrementAndGet();
                log.debug(
                        "[CACHE-WARM:{}] failed ns={}, mci={}, vm={}, m={}, err={}",
                        jobName,
                        vm.nsId(),
                        vm.mciId(),
                        vm.vmId(),
                        measurement,
                        e.toString());
            }
        }
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

    private MetricRequestDTO buildRequest(Job job, String measurement) {
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement(measurement);
        req.setRange(job.getRange());
        req.setGroupTime(job.getGroupTime());
        req.setFields(new ArrayList<>());
        req.setConditions(new ArrayList<>());
        return req;
    }

    private static ExecutorService newFixedPool(String namePrefix, Job job) {
        int size = Math.max(1, job.getThreadPoolSize());
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory =
                r -> {
                    Thread t = new Thread(r, namePrefix + "-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                };
        return Executors.newFixedThreadPool(size, factory);
    }

    private record VmWithCreatedAt(VmRef vm, Instant createdAt) {}
}
