package com.mcmp.o11ymanager.manager.infrastructure.log.mapper;

import com.mcmp.o11ymanager.manager.infrastructure.log.dto.LokiVolumeResponseDto;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LokiVolumeResponseMapper {

    /**
     * @param dto
     * @return
     */
    public static LogVolume toDomain(LokiVolumeResponseDto dto) {
        if (dto == null) {
            return LogVolume.builder()
                    .status("failure")
                    .data(
                            LogVolume.LogVolumeData.builder()
                                    .resultType(null)
                                    .result(Collections.emptyList())
                                    .build())
                    .build();
        }

        List<LogVolume.MetricResult> results = new ArrayList<>();
        if (dto.getData() != null && dto.getData().getResult() != null) {
            results =
                    dto.getData().getResult().stream()
                            .map(LokiVolumeResponseMapper::mapToMetricResult)
                            .collect(Collectors.toList());
        }

        LogVolume.LogVolumeData data =
                LogVolume.LogVolumeData.builder()
                        .resultType(dto.getData() != null ? dto.getData().getResultType() : null)
                        .result(results)
                        .build();

        return LogVolume.builder().status(dto.getStatus()).data(data).build();
    }

    /**
     * @param resultDto
     * @return
     */
    private static LogVolume.MetricResult mapToMetricResult(
            LokiVolumeResponseDto.VolumeResultDto resultDto) {
        List<LogVolume.TimeSeriesValue> values = new ArrayList<>();
        if (resultDto.getValues() != null) {
            values =
                    resultDto.getValues().stream()
                            .map(LokiVolumeResponseMapper::mapToTimeSeriesValue)
                            .collect(Collectors.toList());
        }

        return LogVolume.MetricResult.builder()
                .metric(resultDto.getMetric())
                .values(values)
                .build();
    }

    /**
     * @param valueArray
     * @return
     */
    private static LogVolume.TimeSeriesValue mapToTimeSeriesValue(List<Object> valueArray) {
        if (valueArray == null || valueArray.size() < 2) {
            return LogVolume.TimeSeriesValue.builder().timestamp(0L).value("0").build();
        }

        Long timestamp =
                valueArray.get(0) instanceof Number
                        ? ((Number) valueArray.get(0)).longValue()
                        : Long.parseLong(valueArray.get(0).toString());

        String value = valueArray.get(1).toString();

        return LogVolume.TimeSeriesValue.builder().timestamp(timestamp).value(value).build();
    }
}
