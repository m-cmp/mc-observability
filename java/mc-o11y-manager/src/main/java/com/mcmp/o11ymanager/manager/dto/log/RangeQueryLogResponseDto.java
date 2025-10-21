package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RangeQueryLogResponseDto {
    private String status;
    private RangeQueryDataDto data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangeQueryDataDto {
        private String resultType;
        private List<MetricResultDto> result;
        private StatsDataDto stats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricResultDto {
        private Map<String, String> metric;
        private List<TimeSeriesValueDto> values;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesValueDto {
        private Long timestamp;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsDataDto {
        private StatsSummaryDto summary;
        private StatsQuerierDto querier;
        private StatsIngesterDto ingester;
        private StatsCacheDto cache;
        private StatsIndexDto index;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsSummaryDto {
        private Long bytesProcessedPerSecond;
        private Long linesProcessedPerSecond;
        private Long totalBytesProcessed;
        private Long totalLinesProcessed;
        private Double execTime;
        private Double queueTime;
        private Integer subqueries;
        private Integer totalEntriesReturned;
        private Integer splits;
        private Integer shards;
        private Long totalPostFilterLines;
        private Long totalStructuredMetadataBytesProcessed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsQuerierDto {
        private StatsStoreDto store;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsIngesterDto {
        private Integer totalReached;
        private Integer totalChunksMatched;
        private Integer totalBatches;
        private Integer totalLinesSent;
        private StatsStoreDto store;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsStoreDto {
        private Integer totalChunksRef;
        private Integer totalChunksDownloaded;
        private Long chunksDownloadTime;
        private Boolean queryReferencedStructuredMetadata;
        private StatsChunkDto chunk;
        private Long chunkRefsFetchTime;
        private Long congestionControlLatency;
        private Integer pipelineWrapperFilteredLines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsChunkDto {
        private Long headChunkBytes;
        private Integer headChunkLines;
        private Long decompressedBytes;
        private Long decompressedLines;
        private Long compressedBytes;
        private Integer totalDuplicates;
        private Long postFilterLines;
        private Long headChunkStructuredMetadataBytes;
        private Long decompressedStructuredMetadataBytes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsCacheDto {
        private StatsCacheEntryDto chunk;
        private StatsCacheEntryDto index;
        private StatsCacheEntryDto result;
        private StatsCacheEntryDto statsResult;
        private StatsCacheEntryDto volumeResult;
        private StatsCacheEntryDto seriesResult;
        private StatsCacheEntryDto labelResult;
        private StatsCacheEntryDto instantMetricResult;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsCacheEntryDto {
        private Integer entriesFound;
        private Integer entriesRequested;
        private Integer entriesStored;
        private Long bytesReceived;
        private Long bytesSent;
        private Integer requests;
        private Long downloadTime;
        private Long queryLengthServed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsIndexDto {
        private Integer totalChunks;
        private Integer postFilterChunks;
        private Long shardsDuration;
        private Boolean usedBloomFilters;
    }
}
