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

        /** Realtime warming — short-range queries refreshed every minute. */
        private Job realtime =
                new Job("0 * * * * *", 10, List.of("cpu", "mem", "disk", "net"), "1h", "1m");

        /** Downsampling warming — long-range queries refreshed on the hourly DAG cycle. */
        private Job downsampling =
                new Job("0 5 * * * *", 10, List.of("cpu", "mem", "disk", "net"), "7d", "1h");
    }

    @Getter
    @Setter
    public static class Job {
        /** Spring cron expression. */
        private String cron;

        /** Number of worker threads for parallel warming. */
        private int threadPoolSize;

        /** Measurements to pre-load. */
        private List<String> measurements;

        /** Time range string passed to the warming query (e.g. {@code 1h}, {@code 7d}). */
        private String range;

        /** Group-by-time interval for the warming query. */
        private String groupTime;

        public Job() {}

        public Job(
                String cron,
                int threadPoolSize,
                List<String> measurements,
                String range,
                String groupTime) {
            this.cron = cron;
            this.threadPoolSize = threadPoolSize;
            this.measurements = measurements;
            this.range = range;
            this.groupTime = groupTime;
        }
    }
}
