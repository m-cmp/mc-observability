package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.log.LabelResponseDto;
import com.mcmp.o11ymanager.manager.dto.log.LabelResultDto;
import com.mcmp.o11ymanager.manager.dto.log.LogResponseDto;
import com.mcmp.o11ymanager.manager.dto.log.LogSummaryDto;
import com.mcmp.o11ymanager.manager.dto.log.LogVolumeResponseDto;
import com.mcmp.o11ymanager.manager.mapper.log.LabelResponseMapper;
import com.mcmp.o11ymanager.manager.mapper.log.LabelResultMapper;
import com.mcmp.o11ymanager.manager.mapper.log.LogResponseMapper;
import com.mcmp.o11ymanager.manager.mapper.log.LogSummaryMapper;
import com.mcmp.o11ymanager.manager.mapper.log.LogVolumeResponseMapper;
import com.mcmp.o11ymanager.manager.model.log.Label;
import com.mcmp.o11ymanager.manager.model.log.Log;
import com.mcmp.o11ymanager.manager.model.log.LogCriteria;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;
import com.mcmp.o11ymanager.manager.service.interfaces.LokiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogFacadeService {

    private final LokiService lokiService;

    /**
     * Retrieve log data within a specific time range (summary response)
     *
     * @param query Log query
     * @param start Start time
     * @param end End time
     * @param limit Query result limit
     * @param direction Query direction (forward/backward)
     * @param interval Minimum time interval between individual log line (stream) results
     * @param step Evaluation interval for metric query (matrix)
     * @param since Time duration from the end (or current) point for query start
     * @return Log summary response DTO
     */
    public LogSummaryDto.ResultDto getRangeLogs(
            String query,
            String start,
            String end,
            int limit,
            String direction,
            String interval,
            String step,
            String since) {
        LogCriteria criteria;

        if ((start == null || start.isEmpty())
                && (end == null || end.isEmpty())
                && (direction == null || direction.isEmpty())
                && (interval == null || interval.isEmpty())
                && (step == null || step.isEmpty())
                && (since == null || since.isEmpty())) {
            criteria = LogCriteria.of(query, limit);
        } else {
            criteria =
                    LogCriteria.ofRange(query, start, end, limit, direction, interval, step, since);
        }

        Log log = lokiService.getRangeLogs(criteria);
        LogResponseDto responseDto = LogResponseMapper.toDto(log);
        return LogSummaryMapper.toResultDto(responseDto, direction);
    }

    /**
     * Retrieve label list (formatted result)
     *
     * @param start Start time (optional)
     * @param end End time (optional)
     * @param query Query (optional)
     * @return Label result response DTO
     */
    public LabelResultDto.LabelsResultDto getLabelResult(String start, String end, String query) {
        LabelResponseDto responseDto = getLabels(start, end, query);
        return LabelResultMapper.toLabelsResultDto(responseDto);
    }

    /**
     * Retrieve value list for a specific label (formatted result)
     *
     * @param label Label name
     * @param start Start time (optional)
     * @param end End time (optional)
     * @param since Time since specific point (optional)
     * @param query Query (optional)
     * @return Label value result response DTO
     */
    public LabelResultDto.LabelValuesResultDto getLabelValuesResult(
            String label, String start, String end, String since, String query) {
        LabelResponseDto responseDto = getLabelValues(label, start, end, since, query);
        return LabelResultMapper.toLabelValuesResultDto(responseDto);
    }

    /**
     * Retrieve label list
     *
     * @param start Start time (optional)
     * @param end End time (optional)
     * @param query Query (optional)
     * @return Label response DTO
     */
    private LabelResponseDto getLabels(String start, String end, String query) {
        Label label = lokiService.getLabels(start, end, query);
        return LabelResponseMapper.toDto(label);
    }

    /**
     * Retrieve values for a specific label
     *
     * @param label Label name
     * @param start Start time (optional)
     * @param end End time (optional)
     * @param since Time since specific point (optional)
     * @param query Query (optional)
     * @return Label value response DTO
     */
    public LabelResponseDto getLabelValues(
            String label, String start, String end, String since, String query) {
        Label labelValues = lokiService.getLabelValues(label, start, end, since, query);
        return LabelResponseMapper.toDto(labelValues);
    }

    /**
     * Retrieve log volume query data
     *
     * @param query Log query
     * @param start Start time
     * @param end End time
     * @param limit Query limit (optional)
     * @return Log volume query response DTO
     */
    public LogVolumeResponseDto getLogVolumes(
            String query, String start, String end, Integer limit) {
        LogVolume logVolume = lokiService.getLogVolumes(query, start, end, limit);
        return LogVolumeResponseMapper.toDto(logVolume);
    }
}
