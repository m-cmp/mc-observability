package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.LogVolumeResponseDto;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LogVolume 도메인 모델을 응용 계층 DTO로 변환하는 매퍼
 */
public class LogVolumeResponseMapper {

    /**
     * 도메인 모델을 DTO로 변환
     * @param logVolume 로그 볼륨 도메인 모델
     * @return 응용 계층 DTO
     */
    public static LogVolumeResponseDto toDto(LogVolume logVolume) {
        if (logVolume == null) {
            return LogVolumeResponseDto.builder()
                    .data(Collections.emptyList())
                    .build();
        }

        List<LogVolumeResponseDto.MetricResultDto> resultDtos = new ArrayList<>();
        if (logVolume.getData() != null && logVolume.getData().getResult() != null) {
            resultDtos = logVolume.getData().getResult().stream()
                    .map(LogVolumeResponseMapper::mapToMetricResultDto)
                    .collect(Collectors.toList());
        }

        return LogVolumeResponseDto.builder()
                .data(resultDtos)
                .build();
    }

    /**
     * 도메인 모델의 MetricResult를 DTO로 변환
     * @param result MetricResult 도메인 객체
     * @return MetricResultDto 응용 계층 DTO
     */
    private static LogVolumeResponseDto.MetricResultDto mapToMetricResultDto(LogVolume.MetricResult result) {
        if (result == null) {
            return LogVolumeResponseDto.MetricResultDto.builder()
                    .metric(Collections.emptyMap())
                    .values(Collections.emptyList())
                    .build();
        }

        List<LogVolumeResponseDto.TimeSeriesValueDto> valueDtos = new ArrayList<>();
        if (result.getValues() != null) {
            valueDtos = result.getValues().stream()
                    .map(LogVolumeResponseMapper::mapToTimeSeriesValueDto)
                    .collect(Collectors.toList());
        }

        return LogVolumeResponseDto.MetricResultDto.builder()
                .metric(result.getMetric())
                .values(valueDtos)
                .build();
    }

    /**
     * 도메인 모델의 TimeSeriesValue를 DTO로 변환
     * @param value TimeSeriesValue 도메인 객체
     * @return TimeSeriesValueDto 응용 계층 DTO
     */
    private static LogVolumeResponseDto.TimeSeriesValueDto mapToTimeSeriesValueDto(LogVolume.TimeSeriesValue value) {
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