package com.mcmp.o11ymanager.manager.service.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.mcmp.o11ymanager.manager.config.MonitoringCacheProperties;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonitoringCacheServiceTest {

    private MonitoringCacheProperties properties;
    private MonitoringCacheService service;

    @BeforeEach
    void setUp() {
        properties = new MonitoringCacheProperties();
        properties.setMinBucketSeconds(60);
        properties.setMaxBucketSeconds(3600);
        properties.setEmptyTtlSeconds(60);
        properties.setHardTtlSeconds(900);
        properties.setRefreshThreads(2);
        service = newService(properties);
    }

    private MonitoringCacheService newService(MonitoringCacheProperties props) {
        MonitoringCacheService s =
                new MonitoringCacheService(props, new QueryAccessTracker(100, 900));
        // @PostConstruct is not run outside the container
        invokeInit(s);
        return s;
    }

    private static void invokeInit(MonitoringCacheService s) {
        try {
            var m = MonitoringCacheService.class.getDeclaredMethod("init");
            m.setAccessible(true);
            m.invoke(s);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static MetricRequestDTO request(String measurement, String range, String groupTime) {
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement(measurement);
        req.setRange(range);
        req.setGroupTime(groupTime);
        return req;
    }

    private static List<MetricDTO> oneSeries() {
        MetricDTO dto =
                new MetricDTO("cpu", List.of("time", "value"), Map.of(), List.of(List.of(1L, 2.0)));
        return List.of(dto);
    }

    @Test
    void secondCallInSameBucketIsServedFromCache() {
        AtomicInteger loads = new AtomicInteger();
        MetricRequestDTO req = request("cpu", "1h", "1m");

        service.getOrLoad("ns", "mci", "vm", req, () -> load(loads));
        service.getOrLoad("ns", "mci", "vm", req, () -> load(loads));

        assertThat(loads.get()).isEqualTo(1);
        assertThat(service.stats()).containsEntry("hitFreshCount", 1L);
    }

    @Test
    void freshWindowFollowsGroupTimeStep() {
        long minuteBucket = MonitoringCacheKey.freshWindowOf(request("cpu", "1h", "1m"), 60, 3600);
        long hourBucket = MonitoringCacheKey.freshWindowOf(request("cpu", "7d", "1h"), 60, 3600);
        long clampedBelow = MonitoringCacheKey.freshWindowOf(request("cpu", "1h", "10s"), 60, 3600);
        long noStep = MonitoringCacheKey.freshWindowOf(request("cpu", "1h", null), 60, 3600);

        assertThat(minuteBucket).isEqualTo(60);
        assertThat(hourBucket).isEqualTo(3600);
        assertThat(clampedBelow).isEqualTo(60);
        assertThat(noStep).isEqualTo(60);
    }

    @Test
    void concurrentMissesOnTheSameKeyLoadOnlyOnce() throws Exception {
        int threads = 16;
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(
                    () -> {
                        try {
                            start.await();
                            service.getOrLoad(
                                    "ns",
                                    "mci",
                                    "vm",
                                    request("cpu", "1h", "1m"),
                                    () -> {
                                        sleep(50);
                                        return load(loads);
                                    });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(loads.get()).isEqualTo(1);
    }

    @Test
    void emptyResultsAreCachedInsteadOfReloadedEveryTime() {
        AtomicInteger loads = new AtomicInteger();
        MetricRequestDTO req = request("dcgm", "1h", "1m");

        service.getOrLoad("ns", "mci", "vm", req, () -> emptyLoad(loads));
        service.getOrLoad("ns", "mci", "vm", req, () -> emptyLoad(loads));
        service.getOrLoad("ns", "mci", "vm", req, () -> emptyLoad(loads));

        assertThat(loads.get()).isEqualTo(1);
        assertThat(service.stats()).containsEntry("emptyCachedCount", 1L);
    }

    @Test
    void refreshNowBypassesTheFreshWindow() {
        AtomicInteger loads = new AtomicInteger();
        MetricRequestDTO req = request("cpu", "1h", "1m");

        service.getOrLoad("ns", "mci", "vm", req, () -> load(loads));
        assertThat(service.isFresh("ns", "mci", "vm", req)).isTrue();

        service.refreshNow("ns", "mci", "vm", req, () -> load(loads));

        assertThat(loads.get()).isEqualTo(2);
    }

    @Test
    void returnedListIsImmutableSoCallersCannotCorruptTheCache() {
        List<MetricDTO> out =
                service.getOrLoad(
                        "ns",
                        "mci",
                        "vm",
                        request("cpu", "1h", "1m"),
                        MonitoringCacheServiceTest::oneSeries);

        assertThat(out).hasSize(1);
        try {
            out.add(null);
            org.junit.jupiter.api.Assertions.fail("cached list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    void staleEntryIsServedImmediatelyAndRefreshedInBackground() throws Exception {
        MonitoringCacheProperties props = new MonitoringCacheProperties();
        props.setMinBucketSeconds(1); // fresh window of one second
        props.setMaxBucketSeconds(1);
        props.setHardTtlSeconds(900);
        props.setRefreshThreads(2);
        MonitoringCacheService shortLived = newService(props);

        AtomicInteger loads = new AtomicInteger();
        MetricRequestDTO req = request("cpu", "1h", "1s");

        shortLived.getOrLoad("ns", "mci", "vm", req, () -> load(loads));
        Thread.sleep(1200);

        long before = System.currentTimeMillis();
        shortLived.getOrLoad(
                "ns",
                "mci",
                "vm",
                req,
                () -> {
                    sleep(300);
                    return load(loads);
                });
        long elapsed = System.currentTimeMillis() - before;

        // the stale copy comes back without waiting for the slow reload
        assertThat(elapsed).isLessThan(250);
        assertThat(shortLived.stats()).containsEntry("hitStaleCount", 1L);

        Thread.sleep(800);
        assertThat(loads.get()).isEqualTo(2); // background refresh happened
    }

    private static List<MetricDTO> load(AtomicInteger counter) {
        counter.incrementAndGet();
        return oneSeries();
    }

    private static List<MetricDTO> emptyLoad(AtomicInteger counter) {
        counter.incrementAndGet();
        return List.of();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
