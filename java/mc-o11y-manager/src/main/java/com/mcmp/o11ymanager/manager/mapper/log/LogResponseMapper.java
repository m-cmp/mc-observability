package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.LogResponseDto;
import com.mcmp.o11ymanager.manager.model.log.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LogResponseMapper {

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
                .data(
                        LogResponseDto.LogDataDto.builder()
                                .resultType(
                                        domain.getLogData() != null
                                                ? domain.getLogData().getResultType()
                                                : null)
                                .results(resultDtos)
                                .stats(
                                        domain.getLogData() != null
                                                ? domain.getLogData().getStats()
                                                : null)
                                .build())
                .build();
    }

    private static LogResponseDto.VectorLogResultDto toVectorDto(Log.VectorLogResult domain) {
        return LogResponseDto.VectorLogResultDto.builder()
                .labels(domain.getLabels())
                .timestamp(domain.getTimestamp())
                .value(domain.getValue())
                .build();
    }

    private static LogResponseDto.StreamLogResultDto toStreamDto(Log.StreamLogResult domain) {
        List<LogResponseDto.StreamLogResultDto.LogEntryDto> entryDtos =
                domain.getEntries() != null
                        ? domain.getEntries().stream()
                                .map(
                                        entry ->
                                                LogResponseDto.StreamLogResultDto.LogEntryDto
                                                        .builder()
                                                        .timestamp(entry.getTimestamp())
                                                        .logLine(entry.getLogLine())
                                                        .build())
                                .collect(Collectors.toList())
                        : new ArrayList<>();

        return LogResponseDto.StreamLogResultDto.builder()
                .labels(domain.getLabels())
                .entries(entryDtos)
                .build();
    }
}
