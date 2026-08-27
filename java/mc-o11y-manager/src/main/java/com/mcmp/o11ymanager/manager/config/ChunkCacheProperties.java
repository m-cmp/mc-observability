package com.mcmp.o11ymanager.manager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for slicing metric queries into cacheable absolute time windows.
 *
 * <p>Off by default. This reduces how much data InfluxDB scans in the background; it does not
 * change what a user waits for, because the result cache already answers from memory and refreshes
 * out of band. Enable it where InfluxDB load is measurably a problem.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "monitoring.cache.chunk")
public class ChunkCacheProperties {

    /** Whether to slice queries at all. */
    private boolean enabled = false;

    /** Roughly how many slices to cut the requested range into. */
    private int targetChunks = 12;

    /** Hard cap on slices per request, so a long range cannot fan out without bound. */
    private int maxChunks = 48;

    /** Ranges shorter than this are not worth slicing. */
    private long minRangeSeconds = 900L;

    /**
     * How long a slice must have been closed before it is cached, covering late-arriving writes.
     * {@code 0} means "one aggregation step".
     */
    private long settleSeconds = 0L;

    /** Maximum number of cached slices. */
    private long maxSlices = 20000L;

    /** Lifetime of a cached slice. */
    private long sliceTtlSeconds = 1800L;
}
