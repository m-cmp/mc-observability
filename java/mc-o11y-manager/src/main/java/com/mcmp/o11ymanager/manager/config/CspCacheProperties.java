package com.mcmp.o11ymanager.manager.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the in-memory CSP monitoring metric cache.
 *
 * <p>Caches cb-spider monitoring responses so repeated NS/MCI overview fetches don't hammer the CSP
 * provider APIs.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "csp.cache")
public class CspCacheProperties {

    /** Whether the CSP cache layer is enabled. */
    private boolean enabled = true;

    /** Maximum number of cached entries. */
    private long maxSize = 10_000L;

    /**
     * Entry TTL after write. 5 minutes comfortably outlives the 1-minute warm cadence for the
     * short-range spec set and the 5-minute cadence for the long-range set, so cached entries stay
     * valid between refreshes.
     */
    private long expireAfterWriteSeconds = 300L;

    /** Periodic cache warming. */
    private Warm warm = new Warm();

    @Getter
    @Setter
    public static class Warm {
        private boolean enabled = true;

        /**
         * Worker thread count for parallel warming. Each warm tick submits (VMs + K8s nodes) ×
         * metric × range tasks that call cb-spider; bigger pool → faster overall warm, bounded
         * cb-spider concurrency.
         */
        private int threadPoolSize = 30;

        /**
         * Maximum number of recently-created VMs (per namespace) to warm on each tick. Older VMs
         * are left to load on-demand.
         */
        private int topN = 20;

        /**
         * Short-range ({@code timeBeforeHour ≤ 12h}) warming — refreshed every minute so the
         * default monitoring view is always close to live.
         */
        private Job realtime =
                new Job(
                        "0 * * * * *",
                        List.of(new Range("1", "5"), new Range("6", "5"), new Range("12", "5")));

        /**
         * Long-range ({@code timeBeforeHour ≥ 24h}) warming — refreshed every 5 minutes since the
         * data changes slowly and each call is expensive (more points, more CSP API round-trips).
         */
        private Job longrange =
                new Job(
                        "0 */5 * * * *",
                        List.of(
                                new Range("24", "5"),
                                new Range("72", "5"),
                                new Range("120", "5"),
                                new Range("168", "5")));
    }

    @Getter
    @Setter
    public static class Job {
        private boolean enabled = true;
        private String cron;
        private List<Range> ranges;

        public Job() {}

        public Job(String cron, List<Range> ranges) {
            this.cron = cron;
            this.ranges = ranges;
        }
    }

    @Getter
    @Setter
    public static class Range {
        /** {@code TimeBeforeHour} query param forwarded to cb-spider. */
        private String timeBeforeHour;

        /** {@code IntervalMinute} query param forwarded to cb-spider. */
        private String intervalMinute;

        public Range() {}

        public Range(String timeBeforeHour, String intervalMinute) {
            this.timeBeforeHour = timeBeforeHour;
            this.intervalMinute = intervalMinute;
        }
    }
}
