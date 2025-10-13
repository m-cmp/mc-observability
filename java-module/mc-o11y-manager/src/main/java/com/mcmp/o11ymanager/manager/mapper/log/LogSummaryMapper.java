package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.LogResponseDto;
import com.mcmp.o11ymanager.manager.dto.log.LogSummaryDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class LogSummaryMapper {

  public static LogSummaryDto.ResultDto toResultDto(LogResponseDto dto, String direction) {
    if (dto == null) {
      return null;
    }

    List<LogSummaryDto.LogEntryDto> entries = new ArrayList<>();

    if (dto.getData() != null && dto.getData().getResults() != null) {
      for (LogResponseDto.LogResultDto result : dto.getData().getResults()) {
        if (result instanceof LogResponseDto.VectorLogResultDto vectorResult) {
          entries.add(
              LogSummaryDto.LogEntryDto.builder()
                  .labels(vectorResult.getLabels())
                  .timestamp(vectorResult.getTimestamp())
                  .value(vectorResult.getValue())
                  .build());
        } else if (result instanceof LogResponseDto.StreamLogResultDto streamResult) {
          if (streamResult.getEntries() != null && !streamResult.getEntries().isEmpty()) {
            for (LogResponseDto.StreamLogResultDto.LogEntryDto entry :
                streamResult.getEntries()) {
              double timestamp = 0;
              try {
                timestamp = Double.parseDouble(entry.getTimestamp());
              } catch (NumberFormatException e) {
                // ignore parse error
              }

              entries.add(
                  LogSummaryDto.LogEntryDto.builder()
                      .labels(streamResult.getLabels())
                      .timestamp(timestamp)
                      .value(entry.getLogLine())
                      .build());
            }
          }
        }
      }
    }

    LogSummaryDto.StatsDto statsDto = extractStats(dto);

    if (StringUtils.isEmpty(direction)) {
      direction = "backward";
    }

    if (direction.equals("forward")) {
      entries =
          entries.stream()
              .sorted(
                  Comparator.comparingDouble(
                      LogSummaryDto.LogEntryDto::getTimestamp))
              .collect(Collectors.toList());
    } else if (direction.equals("backward")) {
      entries =
          entries.stream()
              .sorted(
                  (e1, e2) ->
                      Double.compare(e2.getTimestamp(), e1.getTimestamp()))
              .collect(Collectors.toList());
    }

    return LogSummaryDto.ResultDto.builder()
        .status(dto.getStatus())
        .data(entries)
        .stats(statsDto)
        .build();
  }

  private static LogSummaryDto.StatsDto extractStats(LogResponseDto dto) {
    if (dto.getData() != null && dto.getData().getStats() != null) {
      try {
        if (dto.getData().getStats() instanceof Map) {
          Map<String, Object> stats = (Map<String, Object>) dto.getData().getStats();
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
        // ignore parsing errors
      }
    }

    return LogSummaryDto.StatsDto.builder()
        .totalBytesProcessed(0L)
        .totalLinesProcessed(0L)
        .execTime(0.0)
        .totalEntriesReturned(0)
        .build();
  }

  private static Long getLongValue(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return 0L;
  }

  private static Double getDoubleValue(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return 0.0;
  }

  private static Integer getIntValue(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return 0;
  }
}
