package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.LogVolumeResponseDto;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class LogVolumeResponseMapper {

  public static LogVolumeResponseDto toDto(LogVolume logVolume) {
    if (logVolume == null) {
      return LogVolumeResponseDto.builder().data(Collections.emptyList()).build();
    }

    List<LogVolumeResponseDto.MetricResultDto> resultDtos = new ArrayList<>();
    if (logVolume.getData() != null && logVolume.getData().getResult() != null) {
      resultDtos =
          logVolume.getData().getResult().stream()
              .map(LogVolumeResponseMapper::mapToMetricResultDto)
              .collect(Collectors.toList());
    }

    return LogVolumeResponseDto.builder().data(resultDtos).build();
  }

  private static LogVolumeResponseDto.MetricResultDto mapToMetricResultDto(
      LogVolume.MetricResult result) {
    if (result == null) {
      return LogVolumeResponseDto.MetricResultDto.builder()
          .metric(Collections.emptyMap())
          .values(Collections.emptyList())
          .build();
    }

    List<LogVolumeResponseDto.TimeSeriesValueDto> valueDtos = new ArrayList<>();
    if (result.getValues() != null) {
      valueDtos =
          result.getValues().stream()
              .map(LogVolumeResponseMapper::mapToTimeSeriesValueDto)
              .collect(Collectors.toList());
    }

    return LogVolumeResponseDto.MetricResultDto.builder()
        .metric(result.getMetric())
        .values(valueDtos)
        .build();
  }

  private static LogVolumeResponseDto.TimeSeriesValueDto mapToTimeSeriesValueDto(
      LogVolume.TimeSeriesValue value) {
    if (value == null) {
      return LogVolumeResponseDto.TimeSeriesValueDto.builder()
          .timestamp(0L)
          .value("0")
          .build();
    }

    return LogVolumeResponseDto.TimeSeriesValueDto.builder()
        .timestamp(value.getTimestamp())
        .value(value.getValue())
        .build();
  }
}
