package com.innogrid.tabcloudit.o11ymanager.infrastructure.log.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Loki 쿼리 범위 API 응답을 매핑하는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LokiRangeQueryResponseDto {
    private String status;
    private RangeQueryDataDto data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangeQueryDataDto {
        private String resultType;
        private List<MetricResultDto> result;
        private StatsDto stats;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricResultDto {
        private Map<String, String> stream;
        private List<List<Object>> values;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsDto {
        private StatsSummaryDto summary;
        private StatsQuerierDto querier;
        private StatsIngesterDto ingester;
        private StatsCacheDto cache;
        private StatsIndexDto index;
    }

    @Data
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsQuerierDto {
        private StatsStoreDto store;
    }

    @Data
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsIndexDto {
        private Integer totalChunks;
        private Integer postFilterChunks;
        private Long shardsDuration;
        private Boolean usedBloomFilters;
    }
} 