package com.mcmp.o11ymanager.manager.infrastructure.log.adapter;

import com.mcmp.o11ymanager.manager.exception.log.LokiTimeRangeExceededException;
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

@Component
@RequiredArgsConstructor
public class LokiClientAdapter implements LokiPort {

    private final LokiFeignClient lokiFeignClient;

    @Override
    public Log fetchLogs(String query, int limit) {

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

            String decodedQuery = decodeHtmlEntities(query);

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
            throw new RuntimeException("Failed to call Loki API: " + e.getMessage());
        }
    }

    @Override
    public Label fetchLabels(String start, String end, String query) {
        try {
            // Decode HTML entities if query is provided
            String decodedQuery = query != null ? decodeHtmlEntities(query) : null;
            // Format date strings
            String formattedStart = start != null ? formatDateForLoki(start) : null;
            String formattedEnd = end != null ? formatDateForLoki(end) : null;

            LokiLabelsResponseDto response =
                    lokiFeignClient
                            .fetchLabels(formattedStart, formattedEnd, decodedQuery)
                            .orElseThrow(
                                    () -> new RuntimeException("Failed to call Loki label API"));

            return LokiLabelsResponseMapper.toDomain(response);
        } catch (FeignException e) {
            if (e.status() == 400
                    && e.contentUTF8().contains("query time range exceeds the limit")) {
                String[] timeRangeInfo = extractTimeRangeError(e.contentUTF8());
                throw new LokiTimeRangeExceededException(
                        UUID.randomUUID().toString(), timeRangeInfo[0], timeRangeInfo[1]);
            }
            throw new RuntimeException("Failed to call Loki label API: " + e.getMessage());
        }
    }

    @Override
    public Label fetchLabelValues(
            String label, String start, String end, String since, String query) {
        try {
            // Decode HTML entities if query is provided
            String decodedQuery = query != null ? decodeHtmlEntities(query) : null;
            // Format date strings
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
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Failed to call Loki label values API"));

            return LokiLabelsResponseMapper.toDomain(response);
        } catch (FeignException e) {
            if (e.status() == 400
                    && e.contentUTF8().contains("query time range exceeds the limit")) {
                // Handle time range limit error
                String[] errorMessage = extractTimeRangeError(e.contentUTF8());
                throw new RuntimeException(
                        "The query time range exceeds the limit of the Loki server. "
                                + errorMessage
                                + " Please try again with a shorter time range.");
            }
            throw new RuntimeException("Failed to call Loki label values API: " + e.getMessage());
        }
    }

    @Override
    public LogVolume fetchLogVolumes(String query, String start, String end, Integer limit) {
        try {
            // Decode HTML entities
            String decodedQuery = decodeHtmlEntities(query);

            // Format date strings
            String formattedStart = formatDateForLoki(start);
            String formattedEnd = formatDateForLoki(end);

            // Dynamically calculate step value
            int step = calculateOptimalStep(formattedStart, formattedEnd);

            String transformedQuery =
                    String.format("sum by (level) (count_over_time(%s [%ds]))", decodedQuery, step);

            LokiVolumeResponseDto response =
                    lokiFeignClient
                            .fetchLogVolumes(
                                    transformedQuery, formattedStart, formattedEnd, step, limit)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Failed to call Loki log range query API"));

            return LokiVolumeResponseMapper.toDomain(response);
        } catch (FeignException e) {
            if (e.status() == 400
                    && e.contentUTF8().contains("query time range exceeds the limit")) {
                String[] timeRangeInfo = extractTimeRangeError(e.contentUTF8());
                throw new LokiTimeRangeExceededException(
                        UUID.randomUUID().toString(), timeRangeInfo[0], timeRangeInfo[1]);
            }
            throw new RuntimeException(
                    "Failed to call Loki log range query API: " + e.getMessage());
        }
    }

    /**
     * Calculates the optimal step value based on the given time range. Ensures that the number of
     * data points does not exceed 10,000.
     *
     * @param startStr Start time (Unix timestamp in nanoseconds)
     * @param endStr End time (Unix timestamp in nanoseconds)
     * @return Optimized step value in seconds
     */
    private int calculateOptimalStep(String startStr, String endStr) {
        try {
            // Convert nanosecond timestamps to seconds
            long startSeconds = Long.parseLong(startStr) / 1_000_000_000L;
            long endSeconds = Long.parseLong(endStr) / 1_000_000_000L;

            long durationSeconds = endSeconds - startSeconds;

            // Set maximum data points to 10,000 (for buffer margin)
            int maxDataPoints = 10_000;

            // Calculate required step (minimum 10 seconds)
            int calculatedStep = Math.max(10, (int) (durationSeconds / maxDataPoints));

            // Round step to the nearest multiple of 10
            calculatedStep = ((calculatedStep + 9) / 10) * 10;

            // Limit maximum step to 3600 seconds (1 hour)
            return Math.min(calculatedStep, 3600);

        } catch (Exception e) {
            // Return default step (30s) if parsing fails
            System.err.println("Failed to calculate step: " + e.getMessage());
            return 30;
        }
    }

    /**
     * Decodes HTML entities in the given string. Example: &quot; → "
     *
     * @param input String to decode
     * @return Decoded string
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
     * Converts a date string into a format compatible with the Loki API. Loki expects Unix
     * timestamps in seconds (converted to nanoseconds internally).
     *
     * @param dateStr Date string to convert
     * @return Unix timestamp string in nanoseconds
     */
    private String formatDateForLoki(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return dateStr;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Check if the format is yyyy-MM-dd HH:mm:ss
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                // Convert to Unix timestamp in nanoseconds
                long nanoSeconds =
                        dateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
                                * 1_000_000L;
                return String.valueOf(nanoSeconds);
            } catch (Exception e) {
                // Check if the string is a Unix timestamp (10 or 13 digits)
                if (dateStr.matches("\\d{10}") || dateStr.matches("\\d{13}")) {
                    try {
                        long timestamp = Long.parseLong(dateStr);
                        // Convert seconds to milliseconds if it's 10 digits
                        if (dateStr.length() == 10) {
                            timestamp = timestamp * 1000;
                        }
                        // Convert to nanoseconds
                        long nanoSeconds = timestamp * 1_000_000L;
                        return String.valueOf(nanoSeconds);
                    } catch (NumberFormatException nfe) {
                        return dateStr;
                    }
                }
                // Return original string if format is invalid
                return dateStr;
            }

        } catch (Exception e) {
            // Return original string if parsing fails (with logging)
            System.err.println("Failed to convert date: " + dateStr + ", error: " + e.getMessage());
            return dateStr;
        }
    }

    /**
     * Extracts detailed time range information from a Loki error message.
     *
     * @param errorMessage Error message returned by Loki
     * @return Array containing [queryLength, limit]
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
        } catch (Exception ignored) {
        }
        return new String[] {"unknown", "unknown"};
    }
}
