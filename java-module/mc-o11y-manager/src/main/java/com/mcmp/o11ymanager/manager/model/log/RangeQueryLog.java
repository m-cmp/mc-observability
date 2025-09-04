package com.mcmp.o11ymanager.manager.model.log;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/** Loki 쿼리 범위 로그 도메인 모델 */
@Getter
@Builder
public class RangeQueryLog {
    private final String status;
    private final RangeQueryData data;

    @Getter
    @Builder
    public static class RangeQueryData {
        private final String resultType;
        private final List<MetricResult> results;
        private final StatsData stats;
    }

    @Getter
    @Builder
    public static class MetricResult {
        private final Map<String, String> metric;
        private final List<TimeSeriesValue> values;
    }

    @Getter
    @Builder
    public static class TimeSeriesValue {
        private final Long timestamp;
        private final String value;
    }

    @Getter
    @Builder
    public static class StatsData {
        private final StatsSummary summary;
        private final StatsQuerier querier;
        private final StatsIngester ingester;
        private final StatsCache cache;
        private final StatsIndex index;
    }

    @Getter
    @Builder
    public static class StatsSummary {
        private final Long bytesProcessedPerSecond;
        private final Long linesProcessedPerSecond;
        private final Long totalBytesProcessed;
        private final Long totalLinesProcessed;
        private final Double execTime;
        private final Double queueTime;
        private final Integer subqueries;
        private final Integer totalEntriesReturned;
        private final Integer splits;
        private final Integer shards;
        private final Long totalPostFilterLines;
        private final Long totalStructuredMetadataBytesProcessed;
    }

    @Getter
    @Builder
    public static class StatsQuerier {
        private final StatsStore store;
    }

    @Getter
    @Builder
    public static class StatsIngester {
        private final Integer totalReached;
        private final Integer totalChunksMatched;
        private final Integer totalBatches;
        private final Integer totalLinesSent;
        private final StatsStore store;
    }

    @Getter
    @Builder
    public static class StatsStore {
        private final Integer totalChunksRef;
        private final Integer totalChunksDownloaded;
        private final Long chunksDownloadTime;
        private final Boolean queryReferencedStructuredMetadata;
        private final StatsChunk chunk;
        private final Long chunkRefsFetchTime;
        private final Long congestionControlLatency;
        private final Integer pipelineWrapperFilteredLines;
    }

    @Getter
    @Builder
    public static class StatsChunk {
        private final Long headChunkBytes;
        private final Integer headChunkLines;
        private final Long decompressedBytes;
        private final Long decompressedLines;
        private final Long compressedBytes;
        private final Integer totalDuplicates;
        private final Long postFilterLines;
        private final Long headChunkStructuredMetadataBytes;
        private final Long decompressedStructuredMetadataBytes;
    }

    @Getter
    @Builder
    public static class StatsCache {
        private final StatsCacheEntry chunk;
        private final StatsCacheEntry index;
        private final StatsCacheEntry result;
        private final StatsCacheEntry statsResult;
        private final StatsCacheEntry volumeResult;
        private final StatsCacheEntry seriesResult;
        private final StatsCacheEntry labelResult;
        private final StatsCacheEntry instantMetricResult;
    }

    @Getter
    @Builder
    public static class StatsCacheEntry {
        private final Integer entriesFound;
        private final Integer entriesRequested;
        private final Integer entriesStored;
        private final Long bytesReceived;
        private final Long bytesSent;
        private final Integer requests;
        private final Long downloadTime;
        private final Long queryLengthServed;
    }

    @Getter
    @Builder
    public static class StatsIndex {
        private final Integer totalChunks;
        private final Integer postFilterChunks;
        private final Long shardsDuration;
        private final Boolean usedBloomFilters;
    }
}
