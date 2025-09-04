package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그 API 응답을 위한 DTO 클래스 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponseDto {
    private String status;
    private LogDataDto data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogDataDto {
        private String resultType;
        private List<? extends LogResultDto> results;
        private Object stats;
    }

    public interface LogResultDto {
        Map<String, String> getLabels();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VectorLogResultDto implements LogResultDto {
        private Map<String, String> labels;
        private double timestamp;
        private String value;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamLogResultDto implements LogResultDto {
        private Map<String, String> labels;
        private List<LogEntryDto> entries;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class LogEntryDto {
            private String timestamp;
            private String logLine;
        }
    }
}
