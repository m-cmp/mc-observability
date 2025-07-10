package com.innogrid.tabcloudit.o11ymanager.mapper.log;

import com.innogrid.tabcloudit.o11ymanager.dto.log.LogResponseDto;
import com.innogrid.tabcloudit.o11ymanager.dto.log.LogSummaryDto;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LogResponseDto를 LogSummaryDto로 변환하는 매퍼
 */
public class LogSummaryMapper {

    /**
     * LogSummaryDto.ResultDto 생성
     */
    public static LogSummaryDto.ResultDto toResultDto(LogResponseDto dto, String direction) {
        if (dto == null) {
            return null;
        }

        List<LogSummaryDto.LogEntryDto> entries = new ArrayList<>();
        
        if (dto.getData() != null && dto.getData().getResults() != null) {
            for (LogResponseDto.LogResultDto result : dto.getData().getResults()) {
                if (result instanceof LogResponseDto.VectorLogResultDto) {
                    // 벡터 타입 결과 처리
                    LogResponseDto.VectorLogResultDto vectorResult = (LogResponseDto.VectorLogResultDto) result;
                    entries.add(LogSummaryDto.LogEntryDto.builder()
                            .labels(vectorResult.getLabels())
                            .timestamp(vectorResult.getTimestamp())
                            .value(vectorResult.getValue())
                            .build());
                } else if (result instanceof LogResponseDto.StreamLogResultDto) {
                    // 스트림 타입 결과 처리
                    LogResponseDto.StreamLogResultDto streamResult = (LogResponseDto.StreamLogResultDto) result;
                    if (streamResult.getEntries() != null && !streamResult.getEntries().isEmpty()) {
                        for (LogResponseDto.StreamLogResultDto.LogEntryDto entry : streamResult.getEntries()) {
                            double timestamp = 0;
                            try {
                                timestamp = Double.parseDouble(entry.getTimestamp());
                            } catch (NumberFormatException e) {
                                // 숫자 변환 실패 시 기본값 사용
                            }
                            
                            entries.add(LogSummaryDto.LogEntryDto.builder()
                                    .labels(streamResult.getLabels())
                                    .timestamp(timestamp)
                                    .value(entry.getLogLine())
                                    .build());
                        }
                    }
                }
            }
        }

        // 통계 정보 추출
        LogSummaryDto.StatsDto statsDto = extractStats(dto);

        // 정렬
        if (StringUtils.isEmpty(direction)) {
            direction = "backward";
        }

        if(direction.equals("forward")) {
            entries = entries.stream()
                    .sorted(Comparator.comparingDouble(LogSummaryDto.LogEntryDto::getTimestamp))
                    .collect(Collectors.toList());
        } else if(direction.equals("backward")) {
            entries = entries.stream()
                    .sorted((e1, e2) -> Double.compare(e2.getTimestamp(), e1.getTimestamp()))
                    .collect(Collectors.toList());
        }

        return LogSummaryDto.ResultDto.builder()
                .status(dto.getStatus())
                .data(entries)
                .stats(statsDto)
                .build();
    }

    /**
     * 통계 정보 추출 메서드
     */
    private static LogSummaryDto.StatsDto extractStats(LogResponseDto dto) {
        if (dto.getData() != null && dto.getData().getStats() != null) {
            try {
                if (dto.getData().getStats() instanceof Map) {
                    Map<String, Object> stats = (Map<String, Object>) dto.getData().getStats();
                    
                    // stats.summary에서 필요한 정보 추출
                    Map<String, Object> summary = (Map<String, Object>) stats.get("summary");
                    if (summary != null) {
                        return LogSummaryDto.StatsDto.builder()
                                .totalBytesProcessed(getLongValue(summary, "totalBytesProcessed"))
                                .totalLinesProcessed(getLongValue(summary, "totalLinesProcessed"))
                                .execTime(getDoubleValue(summary, "execTime"))
                                .totalEntriesReturned(getIntValue(summary, "totalEntriesReturned"))
                                .build();
                    }
                }
            } catch (Exception e) {
                // 예외 발생 시 빈 통계 정보 반환
            }
        }
        
        // 기본 통계 정보 생성
        return LogSummaryDto.StatsDto.builder()
                .totalBytesProcessed(0L)
                .totalLinesProcessed(0L)
                .execTime(0.0)
                .totalEntriesReturned(0)
                .build();
    }

    /**
     * Map에서 Long 값 추출
     */
    private static Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * Map에서 Double 값 추출
     */
    private static Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Map에서 Integer 값 추출
     */
    private static Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
} 