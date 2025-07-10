package com.innogrid.tabcloudit.o11ymanager.model.log;

import lombok.Builder;
import lombok.Getter;

/**
 * 로그 검색 조건을 나타내는 도메인 모델
 */
@Getter
@Builder
public class LogCriteria {
    private final String query;
    private final int limit;
    private final String start;
    private final String end;
    private final String direction;
    private final String interval;
    private final String step;
    private final String since;

    /**
     * 기본 로그 검색 조건 생성
     */
    public static LogCriteria of(String query, int limit) {
        return LogCriteria.builder()
                .query(query)
                .limit(limit)
                .build();
    }

    /**
     * 시간 범위가 있는 로그 검색 조건 생성
     */
    public static LogCriteria ofRange(String query, String start, String end, int limit, String direction,
                                      String interval, String step, String since) {
        return LogCriteria.builder()
                .query(query)
                .start(start)
                .end(end)
                .limit(limit)
                .direction(direction)
                .interval(interval)
                .step(step)
                .since(since)
                .build();
    }
} 