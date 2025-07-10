package com.innogrid.tabcloudit.o11ymanager.mapper.log;

import com.innogrid.tabcloudit.o11ymanager.model.log.Log;
import com.innogrid.tabcloudit.o11ymanager.dto.log.LogResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 도메인 모델을 프레젠테이션 계층 DTO로 변환하는 매퍼
 */
public class LogResponseMapper {

    /**
     * 도메인 모델을 프레젠테이션 DTO로 변환
     */
    public static LogResponseDto toDto(Log domain) {
        if (domain == null) {
            return null;
        }

        List<LogResponseDto.LogResultDto> resultDtos = new ArrayList<>();

        if (domain.getLogData() != null && domain.getLogData().getResults() != null) {
            for (Log.LogResult result : domain.getLogData().getResults()) {
                if (result instanceof Log.VectorLogResult) {
                    resultDtos.add(toVectorDto((Log.VectorLogResult) result));
                } else if (result instanceof Log.StreamLogResult) {
                    resultDtos.add(toStreamDto((Log.StreamLogResult) result));
                }
            }
        }

        return LogResponseDto.builder()
                .status(domain.getStatus())
                .data(LogResponseDto.LogDataDto.builder()
                        .resultType(domain.getLogData() != null ? domain.getLogData().getResultType() : null)
                        .results(resultDtos)
                        .stats(domain.getLogData() != null ? domain.getLogData().getStats() : null)
                        .build())
                .build();
    }

    /**
     * 벡터 타입 도메인 모델을 DTO로 변환
     */
    private static LogResponseDto.VectorLogResultDto toVectorDto(Log.VectorLogResult domain) {
        return LogResponseDto.VectorLogResultDto.builder()
                .labels(domain.getLabels())
                .timestamp(domain.getTimestamp())
                .value(domain.getValue())
                .build();
    }

    /**
     * 스트림 타입 도메인 모델을 DTO로 변환
     */
    private static LogResponseDto.StreamLogResultDto toStreamDto(Log.StreamLogResult domain) {
        List<LogResponseDto.StreamLogResultDto.LogEntryDto> entryDtos = 
            domain.getEntries() != null ? 
                domain.getEntries().stream()
                    .map(entry -> LogResponseDto.StreamLogResultDto.LogEntryDto.builder()
                            .timestamp(entry.getTimestamp())
                            .logLine(entry.getLogLine())
                            .build())
                    .collect(Collectors.toList()) : 
                new ArrayList<>();
        
        return LogResponseDto.StreamLogResultDto.builder()
                .labels(domain.getLabels())
                .entries(entryDtos)
                .build();
    }
} 