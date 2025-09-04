package com.mcmp.o11ymanager.manager.infrastructure.log.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Loki API 응답을 매핑하는 DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LokiResponseDto {
    private String status;
    private LokiDataDto data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LokiDataDto {
        private String resultType;
        private List<Object> result;
        private Object stats;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VectorValueDto {
        private Map<String, String> metric;
        private List<Object> value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamValueDto {
        private Map<String, String> stream;
        private List<List<String>> values;
    }
}
