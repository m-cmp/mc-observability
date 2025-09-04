package com.mcmp.o11ymanager.manager.model.log;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/** 로그 응답을 나타내는 도메인 모델 */
@Getter
@Builder
public class Log {
    private final String status;
    private final LogData logData;

    /** 응답이 성공인지 확인 */
    public boolean isSuccess() {
        return "success".equals(status);
    }

    /** 로그 데이터를 나타내는 내부 클래스 */
    @Getter
    @Builder
    public static class LogData {
        private final String resultType;
        private final List<LogResult> results;
        private final Object stats;

        /** 결과 타입이 벡터인지 확인 */
        public boolean isVectorType() {
            return "vector".equals(resultType);
        }

        /** 결과 타입이 스트림인지 확인 */
        public boolean isStreamsType() {
            return "streams".equals(resultType);
        }
    }

    /** 로그 결과를 나타내는 인터페이스 */
    public interface LogResult {
        /** 로그에 연관된 라벨 정보 반환 */
        Map<String, String> getLabels();
    }

    /** 벡터 타입 로그 결과 */
    @Getter
    @Builder
    public static class VectorLogResult implements LogResult {
        private final Map<String, String> labels;
        private final double timestamp;
        private final String value;

        @Override
        public Map<String, String> getLabels() {
            return labels != null ? labels : Collections.emptyMap();
        }
    }

    /** 스트림 타입 로그 결과 */
    @Getter
    @Builder
    public static class StreamLogResult implements LogResult {
        private final Map<String, String> labels;
        private final List<LogEntry> entries;

        @Override
        public Map<String, String> getLabels() {
            return labels != null ? labels : Collections.emptyMap();
        }

        /** 로그 엔트리 */
        @Getter
        @Builder
        public static class LogEntry {
            private final String timestamp;
            private final String logLine;
        }
    }
}
