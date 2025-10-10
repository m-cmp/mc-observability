package com.mcmp.o11ymanager.manager.infrastructure.log.mapper;

import com.mcmp.o11ymanager.manager.infrastructure.log.dto.LokiResponseDto;
import com.mcmp.o11ymanager.manager.model.log.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LokiResponseMapper {

    public static Log toDomain(LokiResponseDto dto) {
        if (dto == null || dto.getData() == null) {
            return Log.builder()
                    .status("failure")
                    .logData(
                            Log.LogData.builder()
                                    .resultType(null)
                                    .results(Collections.emptyList())
                                    .build())
                    .build();
        }

        List<Log.LogResult> results = new ArrayList<>();

        LokiResponseDto.LokiDataDto data = dto.getData();
        String resultType = data.getResultType();

        if ("vector".equals(resultType)) {
            results = extractVectorResults(data);
        } else if ("streams".equals(resultType)) {
            results = extractStreamResults(data);
        }

        return Log.builder()
                .status(dto.getStatus())
                .logData(
                        Log.LogData.builder()
                                .resultType(resultType)
                                .results(results)
                                .stats(data.getStats())
                                .build())
                .build();
    }

    /** Extract vector type results */
    @SuppressWarnings("unchecked")
    private static List<Log.LogResult> extractVectorResults(LokiResponseDto.LokiDataDto data) {
        if (data.getResult() == null) {
            return Collections.emptyList();
        }

        return data.getResult().stream()
                .filter(item -> item instanceof Map)
                .map(
                        item -> {
                            Map<String, Object> map = (Map<String, Object>) item;

                            // Convert to LokiResponseDto.VectorValueDto
                            LokiResponseDto.VectorValueDto vectorDto =
                                    new LokiResponseDto.VectorValueDto();
                            vectorDto.setMetric((Map<String, String>) map.get("metric"));
                            vectorDto.setValue((List<Object>) map.get("value"));

                            return toVectorLogResult(vectorDto);
                        })
                .collect(Collectors.toList());
    }

    /** Extract stream type results */
    @SuppressWarnings("unchecked")
    private static List<Log.LogResult> extractStreamResults(LokiResponseDto.LokiDataDto data) {
        if (data.getResult() == null) {
            return Collections.emptyList();
        }

        return data.getResult().stream()
                .filter(item -> item instanceof Map)
                .map(
                        item -> {
                            Map<String, Object> map = (Map<String, Object>) item;

                            LokiResponseDto.StreamValueDto streamDto =
                                    new LokiResponseDto.StreamValueDto();
                            streamDto.setStream((Map<String, String>) map.get("stream"));
                            streamDto.setValues((List<List<String>>) map.get("values"));

                            return toStreamLogResult(streamDto);
                        })
                .collect(Collectors.toList());
    }

    private static Log.VectorLogResult toVectorLogResult(LokiResponseDto.VectorValueDto dto) {
        if (dto == null) {
            return Log.VectorLogResult.builder().build();
        }

        double timestamp = 0;
        String value = "";

        if (dto.getValue() != null && !dto.getValue().isEmpty()) {
            if (dto.getValue().size() > 0) {
                Object timeObj = dto.getValue().get(0);
                timestamp = timeObj != null ? Double.parseDouble(timeObj.toString()) : 0;
            }

            if (dto.getValue().size() > 1) {
                Object valueObj = dto.getValue().get(1);
                value = valueObj != null ? valueObj.toString() : "";
            }
        }

        return Log.VectorLogResult.builder()
                .labels(dto.getMetric())
                .timestamp(timestamp)
                .value(value)
                .build();
    }

    private static Log.StreamLogResult toStreamLogResult(LokiResponseDto.StreamValueDto dto) {
        if (dto == null) {
            return Log.StreamLogResult.builder().build();
        }

        List<Log.StreamLogResult.LogEntry> entries = new ArrayList<>();

        if (dto.getValues() != null) {
            for (List<String> value : dto.getValues()) {
                if (value != null && !value.isEmpty()) {
                    String timestamp = value.size() > 0 ? value.get(0) : "";
                    String logLine = value.size() > 1 ? value.get(1) : "";

                    entries.add(
                            Log.StreamLogResult.LogEntry.builder()
                                    .timestamp(timestamp)
                                    .logLine(logLine)
                                    .build());
                }
            }
        }

        return Log.StreamLogResult.builder().labels(dto.getStream()).entries(entries).build();
    }
}
