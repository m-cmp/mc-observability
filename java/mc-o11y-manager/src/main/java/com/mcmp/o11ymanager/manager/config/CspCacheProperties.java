package com.mcmp.o11ymanager.manager.config;

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
     * Entry TTL after write. CSP providers typically publish metrics on a 1–5 minute cadence, so a
     * 60-second TTL keeps results fresh while still absorbing tab-switch storms.
     */
    private long expireAfterWriteSeconds = 60L;

    /** Periodic cache warming. */
    private Warm warm = new Warm();

    @Getter
    @Setter
    public static class Warm {
        private boolean enabled = true;
        private String cron = "0 * * * * *";

        /**
         * Worker thread count for parallel warming. Since warm tasks are synchronous cb-spider
         * calls, this also bounds concurrent outbound requests.
         */
        private int threadPoolSize = 10;

        /** Default query parameters for warm fetches. */
        private String timeBeforeHour = "1";

        private String intervalMinute = "5";

        /**
         * Maximum number of recently-created VMs (per namespace) to warm on each tick. Older VMs
         * are left to load on-demand.
         */
        private int topN = 20;
    }
}
