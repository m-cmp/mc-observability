package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.RangeQueryLogResponseDto;
import com.mcmp.o11ymanager.manager.model.log.RangeQueryLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 쿼리 범위 로그 도메인 모델을 응용 계층 DTO로 변환하는 매퍼
 */
public class RangeQueryLogResponseMapper {

    /**
     * 도메인 모델을 DTO로 변환
     * @param rangeQueryLog 쿼리 범위 로그 도메인 모델
     * @return 응용 계층 DTO
     */
    public static RangeQueryLogResponseDto toDto(RangeQueryLog rangeQueryLog) {
        if (rangeQueryLog == null) {
            return null;
        }

        RangeQueryLogResponseDto.RangeQueryDataDto dataDto = null;
        if (rangeQueryLog.getData() != null) {
            List<RangeQueryLogResponseDto.MetricResultDto> resultDtos = new ArrayList<>();
            if (rangeQueryLog.getData().getResults() != null) {
                resultDtos = rangeQueryLog.getData().getResults().stream()
                        .map(RangeQueryLogResponseMapper::mapToMetricResultDto)
                        .collect(Collectors.toList());
            }

            dataDto = RangeQueryLogResponseDto.RangeQueryDataDto.builder()
                    .resultType(rangeQueryLog.getData().getResultType())
                    .result(resultDtos)
                    .stats(mapToStatsDataDto(rangeQueryLog.getData().getStats()))
                    .build();
        }

        return RangeQueryLogResponseDto.builder()
                .status(rangeQueryLog.getStatus())
                .data(dataDto)
                .build();
    }

    private static RangeQueryLogResponseDto.MetricResultDto mapToMetricResultDto(RangeQueryLog.MetricResult result) {
        if (result == null) {
            return RangeQueryLogResponseDto.MetricResultDto.builder()
                    .metric(Collections.emptyMap())
                    .values(Collections.emptyList())
                    .build();
        }

        List<RangeQueryLogResponseDto.TimeSeriesValueDto> valueDtos = new ArrayList<>();
        if (result.getValues() != null) {
            valueDtos = result.getValues().stream()
                    .map(RangeQueryLogResponseMapper::mapToTimeSeriesValueDto)
                    .collect(Collectors.toList());
        }

        return RangeQueryLogResponseDto.MetricResultDto.builder()
                .metric(result.getMetric())
                .values(valueDtos)
                .build();
    }

    private static RangeQueryLogResponseDto.TimeSeriesValueDto mapToTimeSeriesValueDto(RangeQueryLog.TimeSeriesValue value) {
        if (value == null) {
            return RangeQueryLogResponseDto.TimeSeriesValueDto.builder()
                    .timestamp(0L)
                    .value("0")
                    .build();
        }

        return RangeQueryLogResponseDto.TimeSeriesValueDto.builder()
                .timestamp(value.getTimestamp())
                .value(value.getValue())
                .build();
    }

    private static RangeQueryLogResponseDto.StatsDataDto mapToStatsDataDto(RangeQueryLog.StatsData stats) {
        if (stats == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsDataDto.builder()
                .summary(mapToStatsSummaryDto(stats.getSummary()))
                .querier(mapToStatsQuerierDto(stats.getQuerier()))
                .ingester(mapToStatsIngesterDto(stats.getIngester()))
                .cache(mapToStatsCacheDto(stats.getCache()))
                .index(mapToStatsIndexDto(stats.getIndex()))
                .build();
    }

    private static RangeQueryLogResponseDto.StatsSummaryDto mapToStatsSummaryDto(RangeQueryLog.StatsSummary summary) {
        if (summary == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsSummaryDto.builder()
                .bytesProcessedPerSecond(summary.getBytesProcessedPerSecond())
                .linesProcessedPerSecond(summary.getLinesProcessedPerSecond())
                .totalBytesProcessed(summary.getTotalBytesProcessed())
                .totalLinesProcessed(summary.getTotalLinesProcessed())
                .execTime(summary.getExecTime())
                .queueTime(summary.getQueueTime())
                .subqueries(summary.getSubqueries())
                .totalEntriesReturned(summary.getTotalEntriesReturned())
                .splits(summary.getSplits())
                .shards(summary.getShards())
                .totalPostFilterLines(summary.getTotalPostFilterLines())
                .totalStructuredMetadataBytesProcessed(summary.getTotalStructuredMetadataBytesProcessed())
                .build();
    }

    private static RangeQueryLogResponseDto.StatsQuerierDto mapToStatsQuerierDto(RangeQueryLog.StatsQuerier querier) {
        if (querier == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsQuerierDto.builder()
                .store(mapToStatsStoreDto(querier.getStore()))
                .build();
    }

    private static RangeQueryLogResponseDto.StatsIngesterDto mapToStatsIngesterDto(RangeQueryLog.StatsIngester ingester) {
        if (ingester == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsIngesterDto.builder()
                .totalReached(ingester.getTotalReached())
                .totalChunksMatched(ingester.getTotalChunksMatched())
                .totalBatches(ingester.getTotalBatches())
                .totalLinesSent(ingester.getTotalLinesSent())
                .store(mapToStatsStoreDto(ingester.getStore()))
                .build();
    }

    private static RangeQueryLogResponseDto.StatsStoreDto mapToStatsStoreDto(RangeQueryLog.StatsStore store) {
        if (store == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsStoreDto.builder()
                .totalChunksRef(store.getTotalChunksRef())
                .totalChunksDownloaded(store.getTotalChunksDownloaded())
                .chunksDownloadTime(store.getChunksDownloadTime())
                .queryReferencedStructuredMetadata(store.getQueryReferencedStructuredMetadata())
                .chunk(mapToStatsChunkDto(store.getChunk()))
                .chunkRefsFetchTime(store.getChunkRefsFetchTime())
                .congestionControlLatency(store.getCongestionControlLatency())
                .pipelineWrapperFilteredLines(store.getPipelineWrapperFilteredLines())
                .build();
    }

    private static RangeQueryLogResponseDto.StatsChunkDto mapToStatsChunkDto(RangeQueryLog.StatsChunk chunk) {
        if (chunk == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsChunkDto.builder()
                .headChunkBytes(chunk.getHeadChunkBytes())
                .headChunkLines(chunk.getHeadChunkLines())
                .decompressedBytes(chunk.getDecompressedBytes())
                .decompressedLines(chunk.getDecompressedLines())
                .compressedBytes(chunk.getCompressedBytes())
                .totalDuplicates(chunk.getTotalDuplicates())
                .postFilterLines(chunk.getPostFilterLines())
                .headChunkStructuredMetadataBytes(chunk.getHeadChunkStructuredMetadataBytes())
                .decompressedStructuredMetadataBytes(chunk.getDecompressedStructuredMetadataBytes())
                .build();
    }

    private static RangeQueryLogResponseDto.StatsCacheDto mapToStatsCacheDto(RangeQueryLog.StatsCache cache) {
        if (cache == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsCacheDto.builder()
                .chunk(mapToStatsCacheEntryDto(cache.getChunk()))
                .index(mapToStatsCacheEntryDto(cache.getIndex()))
                .result(mapToStatsCacheEntryDto(cache.getResult()))
                .statsResult(mapToStatsCacheEntryDto(cache.getStatsResult()))
                .volumeResult(mapToStatsCacheEntryDto(cache.getVolumeResult()))
                .seriesResult(mapToStatsCacheEntryDto(cache.getSeriesResult()))
                .labelResult(mapToStatsCacheEntryDto(cache.getLabelResult()))
                .instantMetricResult(mapToStatsCacheEntryDto(cache.getInstantMetricResult()))
                .build();
    }

    private static RangeQueryLogResponseDto.StatsCacheEntryDto mapToStatsCacheEntryDto(RangeQueryLog.StatsCacheEntry entry) {
        if (entry == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsCacheEntryDto.builder()
                .entriesFound(entry.getEntriesFound())
                .entriesRequested(entry.getEntriesRequested())
                .entriesStored(entry.getEntriesStored())
                .bytesReceived(entry.getBytesReceived())
                .bytesSent(entry.getBytesSent())
                .requests(entry.getRequests())
                .downloadTime(entry.getDownloadTime())
                .queryLengthServed(entry.getQueryLengthServed())
                .build();
    }

    private static RangeQueryLogResponseDto.StatsIndexDto mapToStatsIndexDto(RangeQueryLog.StatsIndex index) {
        if (index == null) {
            return null;
        }

        return RangeQueryLogResponseDto.StatsIndexDto.builder()
                .totalChunks(index.getTotalChunks())
                .postFilterChunks(index.getPostFilterChunks())
                .shardsDuration(index.getShardsDuration())
                .usedBloomFilters(index.getUsedBloomFilters())
                .build();
    }
} 