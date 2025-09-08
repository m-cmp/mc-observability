package com.mcmp.o11ymanager.manager.infrastructure.log.adapter;

import com.mcmp.o11ymanager.exception.log.LokiTimeRangeExceededException;
import com.mcmp.o11ymanager.manager.infrastructure.log.client.LokiFeignClient;
import com.mcmp.o11ymanager.manager.infrastructure.log.dto.LokiLabelsResponseDto;
import com.mcmp.o11ymanager.manager.infrastructure.log.dto.LokiResponseDto;
import com.mcmp.o11ymanager.manager.infrastructure.log.dto.LokiVolumeResponseDto;
import com.mcmp.o11ymanager.manager.infrastructure.log.mapper.LokiLabelsResponseMapper;
import com.mcmp.o11ymanager.manager.infrastructure.log.mapper.LokiResponseMapper;
import com.mcmp.o11ymanager.manager.infrastructure.log.mapper.LokiVolumeResponseMapper;
import com.mcmp.o11ymanager.manager.model.log.Label;
import com.mcmp.o11ymanager.manager.model.log.Log;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;
import com.mcmp.o11ymanager.manager.port.LokiPort;
import feign.FeignException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Loki 레포지토리 어댑터 도메인의 포트 인터페이스를 구현하여 인프라 계층의 구현체와 연결 */
@Component
@RequiredArgsConstructor
public class LokiClientAdapter implements LokiPort {

    private final LokiFeignClient lokiFeignClient;

    @Override
    public Log fetchLogs(String query, int limit) {
        // HTML 엔티티를 디코딩
        String decodedQuery = decodeHtmlEntities(query);
        LokiResponseDto response =
                lokiFeignClient
                        .fetchLogs(decodedQuery, limit)
                        .orElseThrow(() -> new RuntimeException("Loki API 호출 실패"));

        return LokiResponseMapper.toDomain(response);
    }

    @Override
    public Log fetchLogs(
            String query,
            String start,
            String end,
            int limit,
            String direction,
            String interval,
            String step,
            String since) {
        try {
            // HTML 엔티티를 디코딩
            String decodedQuery = decodeHtmlEntities(query);
            // 날짜 형식 변환
            String formattedStart = formatDateForLoki(start);
            String formattedEnd = formatDateForLoki(end);

            LokiResponseDto response =
                    lokiFeignClient
                            .fetchLogsWithRange(
                                    decodedQuery,
                                    formattedStart,
                                    formattedEnd,
                                    limit,
                                    direction,
                                    interval,
                                    step,
                                    since)
                            .orElseThrow(() -> new RuntimeException("Loki API 호출 실패"));

            return LokiResponseMapper.toDomain(response);
        } catch (FeignException e) {
            if (e.status() == 400
                    && e.contentUTF8().contains("query time range exceeds the limit")) {
                String[] timeRangeInfo = extractTimeRangeError(e.contentUTF8());
                throw new LokiTimeRangeExceededException(
                        UUID.randomUUID().toString(), timeRangeInfo[0], timeRangeInfo[1]);
            }
            throw new RuntimeException("Loki API 호출 실패: " + e.getMessage());
        }
    }

    @Override
    public Label fetchLabels(String start, String end, String query) {
        try {
            // 쿼리가 있는 경우 HTML 엔티티 디코딩
            String decodedQuery = query != null ? decodeHtmlEntities(query) : null;
            // 날짜 형식 변환
            String formattedStart = start != null ? formatDateForLoki(start) : null;
            String formattedEnd = end != null ? formatDateForLoki(end) : null;

            LokiLabelsResponseDto response =
                    lokiFeignClient
                            .fetchLabels(formattedStart, formattedEnd, decodedQuery)
                            .orElseThrow(() -> new RuntimeException("Loki 레이블 API 호출 실패"));

            return LokiLabelsResponseMapper.toDomain(response);
        } catch (FeignException e) {
            if (e.status() == 400
                    && e.contentUTF8().contains("query time range exceeds the limit")) {
                String[] timeRangeInfo = extractTimeRangeError(e.contentUTF8());
                throw new LokiTimeRangeExceededException(
                        UUID.randomUUID().toString(), timeRangeInfo[0], timeRangeInfo[1]);
            }
            throw new RuntimeException("Loki 레이블 API 호출 실패: " + e.getMessage());
        }
    }

    @Override
    public Label fetchLabelValues(
            String label, String start, String end, String since, String query) {
        try {
            // 쿼리가 있는 경우 HTML 엔티티 디코딩
            String decodedQuery = query != null ? decodeHtmlEntities(query) : null;
            // 날짜 형식 변환
            String formattedStart = start != null ? formatDateForLoki(start) : null;
            String formattedEnd = end != null ? formatDateForLoki(end) : null;
            String formattedSince = since != null ? formatDateForLoki(since) : null;

            LokiLabelsResponseDto response =
                    lokiFeignClient
                            .fetchLabelValues(
                                    label,
                                    formattedStart,
                                    formattedEnd,
                                    formattedSince,
                                    decodedQuery)
                            .orElseThrow(() -> new RuntimeException("Loki 레이블 값 API 호출 실패"));

            return LokiLabelsResponseMapper.toDomain(response);
        } catch (FeignException e) {
            if (e.status() == 400
                    && e.contentUTF8().contains("query time range exceeds the limit")) {
                // 시간 범위 제한 에러 처리
                String[] errorMessage = extractTimeRangeError(e.contentUTF8());
                throw new RuntimeException(
                        "조회 시간 범위가 Loki 서버의 제한을 초과했습니다. "
                                + errorMessage
                                + " 더 짧은 시간 범위로 다시 시도해주세요.");
            }
            throw new RuntimeException("Loki 레이블 값 API 호출 실패: " + e.getMessage());
        }
    }

    @Override
    public LogVolume fetchLogVolumes(String query, String start, String end, Integer limit) {
        try {
            // HTML 엔티티를 디코딩
            String decodedQuery = decodeHtmlEntities(query);

            // 날짜 형식 변환
            String formattedStart = formatDateForLoki(start);
            String formattedEnd = formatDateForLoki(end);

            // step 값을 동적으로 계산
            int step = calculateOptimalStep(formattedStart, formattedEnd);

            String transformedQuery =
                    String.format(
                            "sum by (level) (count_over_time(%s [%ds] ))", decodedQuery, step);

            LokiVolumeResponseDto response =
                    lokiFeignClient
                            .fetchLogVolumes(
                                    transformedQuery, formattedStart, formattedEnd, step, limit)
                            .orElseThrow(() -> new RuntimeException("Loki 로그 범위 쿼리 API 호출 실패"));

            return LokiVolumeResponseMapper.toDomain(response);
        } catch (FeignException e) {
            if (e.status() == 400
                    && e.contentUTF8().contains("query time range exceeds the limit")) {
                String[] timeRangeInfo = extractTimeRangeError(e.contentUTF8());
                throw new LokiTimeRangeExceededException(
                        UUID.randomUUID().toString(), timeRangeInfo[0], timeRangeInfo[1]);
            }
            throw new RuntimeException("Loki 로그 범위 쿼리 API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * 시간 범위에 따라 최적의 step 값을 계산 최대 11,000개의 데이터 포인트를 넘지 않도록 조절
     *
     * @param startStr 시작 시간 (나노초 Unix timestamp)
     * @param endStr 종료 시간 (나노초 Unix timestamp)
     * @return 최적화된 step 값 (초 단위)
     */
    private int calculateOptimalStep(String startStr, String endStr) {
        try {
            // 나노초 타임스탬프를 초 단위로 변환
            long startSeconds = Long.parseLong(startStr) / 1_000_000_000L;
            long endSeconds = Long.parseLong(endStr) / 1_000_000_000L;

            long durationSeconds = endSeconds - startSeconds;

            // 최대 데이터 포인트 수를 10,000개로 설정 (여유분 확보)
            int maxDataPoints = 10000;

            // 필요한 step 계산 (최소 10초)
            int calculatedStep = Math.max(10, (int) (durationSeconds / maxDataPoints));

            // step을 10의 배수로 반올림
            calculatedStep = ((calculatedStep + 9) / 10) * 10;

            // 최대 step은 3600초(1시간)로 제한
            return Math.min(calculatedStep, 3600);

        } catch (Exception e) {
            // 파싱 실패시 기본값 30초 반환
            System.err.println("Step 계산 실패: " + e.getMessage());
            return 30;
        }
    }

    /**
     * HTML 엔티티를 디코딩하는 유틸리티 메서드 예: &quot; -> "
     *
     * @param input 디코딩할 문자열
     * @return 디코딩된 문자열
     */
    private String decodeHtmlEntities(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input.replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&apos;", "'");
    }

    /**
     * 날짜 형식을 Loki API에서 처리할 수 있는 형식으로 변환하는 메서드 Loki는 Unix 타임스탬프(초 단위)를 기대합니다.
     *
     * @param dateStr 변환할 날짜 문자열
     * @return Unix 타임스탬프 문자열
     */
    private String formatDateForLoki(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return dateStr;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // yyyy-MM-dd HH:mm:ss 형식인지 체크
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                // Unix 타임스탬프 나노초로 변환
                long nanoSeconds =
                        dateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
                                * 1_000_000L;
                return String.valueOf(nanoSeconds);
            } catch (Exception e) {
                // Unix 타임스탬프인지 확인 (숫자로만 구성되어 있고 10자리 또는 13자리)
                if (dateStr.matches("\\d{10}") || dateStr.matches("\\d{13}")) {
                    try {
                        long timestamp = Long.parseLong(dateStr);
                        // 10자리면 초 단위, 13자리면 밀리초 단위
                        if (dateStr.length() == 10) {
                            timestamp = timestamp * 1000; // 초를 밀리초로 변환
                        }
                        // Unix 타임스탬프를 Asia/Seoul 타임존 기준 나노초로 변환
                        long nanoSeconds = timestamp * 1_000_000L;
                        return String.valueOf(nanoSeconds);
                    } catch (NumberFormatException nfe) {
                        return dateStr;
                    }
                }
                // 해당 형식이 아닌 경우 원본 반환
                return dateStr;
            }

        } catch (Exception e) {
            // 날짜 파싱 실패시 원본 문자열 반환 (로깅 추가)
            System.err.println("날짜 변환 실패: " + dateStr + ", 오류: " + e.getMessage());
            return dateStr;
        }
    }

    /**
     * 시간 범위 초과 에러 메시지에서 상세 정보 추출
     *
     * @param errorMessage 에러 메시지 문자열
     * @return [queryLength, limit] 배열
     */
    private String[] extractTimeRangeError(String errorMessage) {
        try {
            if (errorMessage.contains("query length:") && errorMessage.contains("limit:")) {
                int queryStart = errorMessage.indexOf("query length:");
                int limitStart = errorMessage.indexOf("limit:");
                int limitEnd = errorMessage.indexOf(")", limitStart);

                if (queryStart != -1 && limitStart != -1 && limitEnd != -1) {
                    String queryLength =
                            errorMessage.substring(queryStart + 13, limitStart - 2).trim();
                    String limit = errorMessage.substring(limitStart + 6, limitEnd).trim();
                    return new String[] {queryLength, limit};
                }
            }
        } catch (Exception e) {
            // 파싱 실패시 기본값 반환
        }
        return new String[] {"unknown", "unknown"};
    }
}
