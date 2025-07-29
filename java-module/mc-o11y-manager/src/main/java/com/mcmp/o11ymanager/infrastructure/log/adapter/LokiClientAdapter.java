package com.mcmp.o11ymanager.infrastructure.log.adapter;

import com.mcmp.o11ymanager.model.log.Label;
import com.mcmp.o11ymanager.model.log.Log;
import com.mcmp.o11ymanager.model.log.LogVolume;
import com.mcmp.o11ymanager.port.LokiPort;
import com.mcmp.o11ymanager.infrastructure.log.client.LokiFeignClient;
import com.mcmp.o11ymanager.infrastructure.log.dto.LokiLabelsResponseDto;
import com.mcmp.o11ymanager.infrastructure.log.dto.LokiResponseDto;
import com.mcmp.o11ymanager.infrastructure.log.dto.LokiVolumeResponseDto;
import com.mcmp.o11ymanager.infrastructure.log.mapper.LokiLabelsResponseMapper;
import com.mcmp.o11ymanager.infrastructure.log.mapper.LokiResponseMapper;
import com.mcmp.o11ymanager.infrastructure.log.mapper.LokiVolumeResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loki 레포지토리 어댑터
 * 도메인의 포트 인터페이스를 구현하여 인프라 계층의 구현체와 연결
 */
@Component
@RequiredArgsConstructor
public class LokiClientAdapter implements LokiPort {

    private final LokiFeignClient lokiFeignClient;

    @Override
    public Log fetchLogs(String query, int limit) {
        // HTML 엔티티를 디코딩
        String decodedQuery = decodeHtmlEntities(query);
        LokiResponseDto response = lokiFeignClient.fetchLogs(decodedQuery, limit)
                .orElseThrow(() -> new RuntimeException("Loki API 호출 실패"));

        return LokiResponseMapper.toDomain(response);
    }

    @Override
    public Log fetchLogs(String query, String start, String end, int limit, String direction,
                         String interval, String step, String since) {
        // HTML 엔티티를 디코딩
        String decodedQuery = decodeHtmlEntities(query);
        // 날짜 형식 변환
        String formattedStart = formatDateForLoki(start);
        String formattedEnd = formatDateForLoki(end);

        LokiResponseDto response = lokiFeignClient.fetchLogsWithRange(decodedQuery, formattedStart, formattedEnd, limit, direction,
                        interval, step, since)
                .orElseThrow(() -> new RuntimeException("Loki API 호출 실패"));

        return LokiResponseMapper.toDomain(response);
    }

    @Override
    public Label fetchLabels(String start, String end, String query) {
        // 쿼리가 있는 경우 HTML 엔티티 디코딩
        String decodedQuery = query != null ? decodeHtmlEntities(query) : null;
        // 날짜 형식 변환
        String formattedStart = start != null ? formatDateForLoki(start) : null;
        String formattedEnd = end != null ? formatDateForLoki(end) : null;

        LokiLabelsResponseDto response = lokiFeignClient.fetchLabels(formattedStart, formattedEnd, decodedQuery)
                .orElseThrow(() -> new RuntimeException("Loki 레이블 API 호출 실패"));

        return LokiLabelsResponseMapper.toDomain(response);
    }

    @Override
    public Label fetchLabelValues(String label, String start, String end, String since, String query) {
        // 쿼리가 있는 경우 HTML 엔티티 디코딩
        String decodedQuery = query != null ? decodeHtmlEntities(query) : null;
        // 날짜 형식 변환
        String formattedStart = start != null ? formatDateForLoki(start) : null;
        String formattedEnd = end != null ? formatDateForLoki(end) : null;
        String formattedSince = since != null ? formatDateForLoki(since) : null;

        LokiLabelsResponseDto response = lokiFeignClient.fetchLabelValues(label, formattedStart, formattedEnd, formattedSince, decodedQuery)
                .orElseThrow(() -> new RuntimeException("Loki 레이블 값 API 호출 실패"));

        return LokiLabelsResponseMapper.toDomain(response);
    }

    @Override
    public LogVolume fetchLogVolumes(String query, String start, String end, Integer limit) {
        // HTML 엔티티를 디코딩
        String decodedQuery = decodeHtmlEntities(query);

        String transformedQuery = String.format("sum by (level) (count_over_time(%s [1h] ))", decodedQuery);

        // 날짜 형식 변환
        String formattedStart = formatDateForLoki(start);
        String formattedEnd = formatDateForLoki(end);

        LokiVolumeResponseDto response = lokiFeignClient.fetchLogVolumes(transformedQuery, formattedStart, formattedEnd, limit)
                .orElseThrow(() -> new RuntimeException("Loki 로그 범위 쿼리 API 호출 실패"));

        return LokiVolumeResponseMapper.toDomain(response);
    }

    /**
     * HTML 엔티티를 디코딩하는 유틸리티 메서드
     * 예: &quot; -> "
     *
     * @param input 디코딩할 문자열
     * @return 디코딩된 문자열
     */
    private String decodeHtmlEntities(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&apos;", "'");
    }

    /**
     * 날짜 형식을 Loki API에서 처리할 수 있는 형식으로 변환하는 메서드
     * Loki는 Unix 타임스탬프(초 단위)를 기대합니다.
     *
     * @param dateStr 변환할 날짜 문자열
     * @return Unix 타임스탬프 문자열
     */
    private String formatDateForLoki(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return dateStr;
        }

        try {
            java.time.format.DateTimeFormatter formatter;
            java.time.ZonedDateTime dateTime;

            // 공백이 포함된 형식 (2025-03-24 17:03:03)
            if (dateStr.contains(" ")) {
                if (dateStr.length() > 19) { // 밀리초 또는 타임존 정보 포함
                    formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS[ XXX][ Z][ z]");
                } else { // 기본 형식
                    formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                }
                dateTime = java.time.LocalDateTime.parse(dateStr, formatter)
                        .atZone(java.time.ZoneOffset.UTC);
            }
            // T가 포함된 ISO 형식 (2025-03-24T17:03:03)
            else if (dateStr.contains("T")) {
                if (dateStr.endsWith("Z")) {
                    dateTime = java.time.ZonedDateTime.parse(dateStr);
                } else if (dateStr.contains("+") || dateStr.contains("-")) {
                    dateTime = java.time.ZonedDateTime.parse(dateStr);
                } else {
                    dateTime = java.time.LocalDateTime.parse(dateStr)
                            .atZone(java.time.ZoneOffset.UTC);
                }
            }
            // 다른 형식일 경우 일단 원본 반환
            else {
                return dateStr;
            }

            // Unix 타임스탬프(초)로 변환
            return String.valueOf(dateTime.toEpochSecond());

        } catch (Exception e) {
            // 날짜 파싱 실패시 원본 문자열 반환 (로깅 추가)
            System.err.println("날짜 변환 실패: " + dateStr + ", 오류: " + e.getMessage());
            return dateStr;
        }
    }
} 