package com.mcmp.o11ymanager.manager.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the in-memory monitoring metric cache.
 *
 * <p>The cache groups metric query results into 1-hour wall-clock buckets so that repeated queries
 * landing in the same hour return immediately without hitting InfluxDB.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "monitoring.cache")
public class MonitoringCacheProperties {

    /** Whether the cache layer is enabled. */
    private boolean enabled = true;

    /** Bucket size in seconds. Defaults to 1 hour. */
    private long blockPeriodSeconds = 3600L;

    /** Maximum total cache weight in megabytes. Defaults to 512MB per the spec. */
    private long maxWeightMb = 512L;

    /** Entry TTL after write. Defaults to 7 days per the spec. */
    private long expireAfterWriteSeconds = 7L * 24L * 3600L;

    /** Estimated bytes per cached time-series data point (used by the weigher). */
    private int estimatedBytesPerPoint = 200;

    /** Periodic cache-warming configuration. */
    private Warm warm = new Warm();

    @Getter
    @Setter
    public static class Warm {
        /** Whether any periodic warming is enabled. */
        private boolean enabled = true;

        /** Maximum number of recently-created VMs to warm per run. */
        private int topN = 10;

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
         * (specific measurement+field+aggregation, group_by=vm_id, limit=2000). Without this, the
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
