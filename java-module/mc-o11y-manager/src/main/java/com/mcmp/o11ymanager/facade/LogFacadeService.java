package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.log.*;
import com.mcmp.o11ymanager.mapper.log.*;
import com.mcmp.o11ymanager.model.log.Label;
import com.mcmp.o11ymanager.model.log.Log;
import com.mcmp.o11ymanager.model.log.LogCriteria;
import com.mcmp.o11ymanager.model.log.LogVolume;
import com.mcmp.o11ymanager.oldService.domain.interfaces.LokiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogFacadeService {

    private final LokiService lokiService;

    /**
     * 특정 기간의 로그 데이터 조회 (요약 응답)
     * @param query 로그 쿼리
     * @param start 시작 시간
     * @param end 종료 시간
     * @param limit 조회 제한 수
     * @param direction 조회 방향(forward/backward)
     * @param interval 개별 로그 라인(스트림) 결과 사이의 최소 시간 간격
     * @param step 메트릭 쿼리(매트릭스) 결과의 계산/평가 시간 간격
     * @param since 쿼리 시작 시점을 end 시점(또는 현재) 기준으로 기간
     * @return 요약된 로그 응답 DTO
     */
    public LogSummaryDto.ResultDto getRangeLogs(String query, String start, String end, int limit, String direction,
                                                String interval, String step, String since) {
        LogCriteria criteria = LogCriteria.ofRange(query, start, end, limit, direction, interval, step, since);
        Log log = lokiService.getRangeLogs(criteria);
        LogResponseDto responseDto = LogResponseMapper.toDto(log);
        return LogSummaryMapper.toResultDto(responseDto, direction);
    }


    /**
     * 레이블 목록 조회 (결과 형식)
     * @param start 시작 시간 (선택)
     * @param end 종료 시간 (선택)
     * @param query 쿼리 (선택)
     * @return 레이블 결과 응답 DTO
     */
    public LabelResultDto.LabelsResultDto getLabelResult(String start, String end, String query) {
        LabelResponseDto responseDto = getLabels(start, end, query);
        return LabelResultMapper.toLabelsResultDto(responseDto);
    }

    /**
     * 특정 레이블의 값 목록 조회 (결과 형식)
     * @param label 레이블 이름
     * @param start 시작 시간 (선택)
     * @param end 종료 시간 (선택)
     * @param since 특정 시점 이후 (선택)
     * @param query 쿼리 (선택)
     * @return 레이블 값 결과 응답 DTO
     */
    public LabelResultDto.LabelValuesResultDto getLabelValuesResult(String label, String start, String end, String since, String query) {
        LabelResponseDto responseDto = getLabelValues(label, start, end, since, query);
        return LabelResultMapper.toLabelValuesResultDto(responseDto);
    }

    /**
     * 레이블 목록 조회
     * @param start 시작 시간 (선택)
     * @param end 종료 시간 (선택)
     * @param query 쿼리 (선택)
     * @return 레이블 응답 DTO
     */
    private LabelResponseDto getLabels(String start, String end, String query) {
        Label label = lokiService.getLabels(start, end, query);
        return LabelResponseMapper.toDto(label);
    }


    /**
     * 특정 레이블의 값 목록 조회
     * @param label 레이블 이름
     * @param start 시작 시간 (선택)
     * @param end 종료 시간 (선택)
     * @param since 특정 시점 이후 (선택)
     * @param query 쿼리 (선택)
     * @return 레이블 값 응답 DTO
     */
    public LabelResponseDto getLabelValues(String label, String start, String end, String since, String query) {
        Label labelValues = lokiService.getLabelValues(label, start, end, since, query);
        return LabelResponseMapper.toDto(labelValues);
    }


    /**
     * 로그 범위 쿼리 데이터 조회
     * @param query 로그 쿼리
     * @param start 시작 시간
     * @param end 종료 시간
     * @param limit 조회 제한 수 (선택)*
     * @return 로그 범위 쿼리 응답 DTO
     */
    public LogVolumeResponseDto getLogVolumes(String query, String start, String end, Integer limit) {
        LogVolume logVolume = lokiService.getLogVolumes(query, start, end, limit);
        return LogVolumeResponseMapper.toDto(logVolume);
    }


}
