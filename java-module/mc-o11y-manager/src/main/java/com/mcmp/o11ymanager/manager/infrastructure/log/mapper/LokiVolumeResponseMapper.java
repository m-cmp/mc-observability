package com.mcmp.o11ymanager.manager.infrastructure.log.mapper;

import com.mcmp.o11ymanager.manager.infrastructure.log.dto.LokiVolumeResponseDto;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Loki 로그 볼륨 API 응답 DTO를 도메인 모델로 변환하는 매퍼 도메인 객체에 response의 data 부분만 매핑 */
public class LokiVolumeResponseMapper {

    /**
     * LokiVolumeResponseDto를 LogVolume 도메인 모델로 변환
     *
     * @param dto 변환할 LokiVolumeResponseDto 객체
     * @return 변환된 LogVolume 도메인 객체
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

        // 응답의 data 부분만 추출하여 도메인 객체로 변환
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
     * VolumeResultDto를 MetricResult 도메인 객체로 변환
     *
     * @param resultDto 변환할 VolumeResultDto 객체
     * @return 변환된 MetricResult 도메인 객체
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
     * 시계열 값 배열을 TimeSeriesValue 도메인 객체로 변환
     *
     * @param valueArray 변환할 값 배열 [timestamp, value]
     * @return 변환된 TimeSeriesValue 도메인 객체
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
