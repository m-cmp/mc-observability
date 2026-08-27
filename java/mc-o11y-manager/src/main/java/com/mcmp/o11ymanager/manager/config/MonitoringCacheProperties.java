package com.mcmp.o11ymanager.manager.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the in-memory monitoring metric cache.
 *
 * <p>Cache entries are keyed by a time bucket whose width follows the query's own {@code
 * group_time} step, clamped between {@link #minBucketSeconds} and {@link #maxBucketSeconds}. Inside
 * that window an entry is served as-is; past it the entry is still returned while a background task
 * refreshes it, until {@link #hardTtlSeconds} elapses and it is dropped entirely.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "monitoring.cache")
public class MonitoringCacheProperties {

    /** Whether the cache layer is enabled. */
    private boolean enabled = true;

    /**
     * Lower bound for the bucket width, in seconds. Also the width used when a request carries no
     * usable {@code group_time}.
     */
    private long minBucketSeconds = 60L;

    /** Upper bound for the bucket width, in seconds. */
    private long maxBucketSeconds = 3600L;

    /**
     * Fresh window for an empty result, in seconds. Kept short so a VM that has just started
     * reporting shows up quickly, but long enough that a genuinely empty measurement stops being
     * re-queried on every request.
     */
    private long emptyTtlSeconds = 60L;

    /**
     * Absolute lifetime of an entry, in seconds. Past the fresh window an entry is still served
     * while it refreshes in the background; this caps how long that may continue when refreshes
     * keep failing.
     */
    private long hardTtlSeconds = 900L;

    /** Worker threads used for background stale-while-revalidate refreshes. */
    private int refreshThreads = 8;

    /** Maximum total cache weight in megabytes. */
    private long maxWeightMb = 512L;

    /** Estimated bytes per cached time-series data point (used by the weigher). */
    private int estimatedBytesPerPoint = 200;

    /** Periodic cache-warming configuration. */
    private Warm warm = new Warm();

    @Getter
    @Setter
    public static class Warm {
        /** Whether any periodic warming is enabled. */
        private boolean enabled = true;

        /** Maximum number of VMs to warm per run. */
        private int topN = 10;

        /**
         * How warming targets are chosen. {@code recently-queried} follows what users actually look
         * at (falling back to creation order until enough traffic is observed); {@code
         * recently-created} keeps the original creation-order behaviour.
         */
        private String selection = "recently-queried";

        /**
         * Upper bound on a single warming pass, in seconds. Work still running past this is
         * abandoned so a slow InfluxDB cannot make passes pile up on each other.
         */
        private long timeboxSeconds = 45L;

        /**
         * Random delay applied before each pass, in seconds. Without it every job fires exactly on
         * the minute boundary and they contend with each other and with bucket rotation.
         */
        private long jitterSeconds = 10L;

        /** Realtime warming — short-range queries (raw DB) refreshed every minute. */
        private Job realtime =
                new Job(
                        "0 * * * * *",
                        10,
                        List.of(
                                new RangeSpec("1h", "1m"),
                                new RangeSpec("6h", "5m"),
                                new RangeSpec("12h", "5m")));

        /**
         * Long-range warming — queries that route to the downsampling DB. Refreshed on the hourly
         * Airflow DAG cycle.
         */
        private Job longrange =
                new Job(
                        "0 5 * * * *",
                        10,
                        List.of(
                                new RangeSpec("1d", "5m"),
                                new RangeSpec("3d", "15m"),
                                new RangeSpec("5d", "30m"),
                                new RangeSpec("7d", "1h")));

        /**
         * Overview warming — warms the exact query shape the NS/MCI overview in the frontend sends
         * (specific measurement+field+aggregation, group_by=node_id, limit=2000). Without this, the
         * generic {@code realtime} job caches {@code SELECT *} responses whose cache key differs
         * from what the UI requests, causing per-visit cache misses.
         */
        private OverviewJob overview = new OverviewJob();
    }

    @Getter
    @Setter
    public static class Job {
        /** Spring cron expression. */
        private String cron;

        /** Number of worker threads for parallel warming. */
        private int threadPoolSize;

        /** (range, group_time) combinations to pre-load on each tick. */
        private List<RangeSpec> ranges;

        public Job() {}

        public Job(String cron, int threadPoolSize, List<RangeSpec> ranges) {
            this.cron = cron;
            this.threadPoolSize = threadPoolSize;
            this.ranges = ranges;
        }
    }

    @Getter
    @Setter
    public static class OverviewJob {
        private boolean enabled = true;
        private String cron = "0 * * * * *";
        private int threadPoolSize = 10;

        /** One entry per chart the overview renders. Defaults mirror the frontend NS/MCI view. */
        private List<OverviewQuery> queries =
                List.of(
                        new OverviewQuery("cpu", "mean", "usage_idle", "1h", "1m", 2000L),
                        new OverviewQuery("mem", "mean", "used_percent", "1h", "1m", 2000L),
                        new OverviewQuery("disk", "mean", "used_percent", "1h", "1m", 2000L));
    }

    @Getter
    @Setter
    public static class OverviewQuery {
        private String measurement;
        private String function;
        private String field;
        private String range;
        private String groupTime;
        private Long limit;

        public OverviewQuery() {}

        public OverviewQuery(
                String measurement,
                String function,
                String field,
                String range,
                String groupTime,
                Long limit) {
            this.measurement = measurement;
            this.function = function;
            this.field = field;
            this.range = range;
            this.groupTime = groupTime;
            this.limit = limit;
        }
    }

    @Getter
    @Setter
    public static class RangeSpec {
        private String range;
        private String groupTime;

        public RangeSpec() {}

        public RangeSpec(String range, String groupTime) {
            this.range = range;
            this.groupTime = groupTime;
        }
    }
}
