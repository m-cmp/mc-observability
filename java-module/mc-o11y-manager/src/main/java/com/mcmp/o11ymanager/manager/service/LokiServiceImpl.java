package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.model.log.Label;
import com.mcmp.o11ymanager.manager.model.log.Log;
import com.mcmp.o11ymanager.manager.model.log.LogCriteria;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;
import com.mcmp.o11ymanager.manager.port.LokiPort;
import com.mcmp.o11ymanager.manager.service.interfaces.LokiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LokiServiceImpl implements LokiService {

    private final LokiPort lokiPort;

    /**
     * 특정 기간의 로그 데이터를 조회합니다.
     *
     * @param criteria 로그 검색 조건
     * @return 로그 응답 객체
     */
    public Log getRangeLogs(LogCriteria criteria) {
        return lokiPort.fetchLogs(
                criteria.getQuery(),
                criteria.getStart(),
                criteria.getEnd(),
                criteria.getLimit(),
                criteria.getDirection(),
                criteria.getInterval(),
                criteria.getStep(),
                criteria.getSince());
    }

    /**
     * 레이블 목록을 조회합니다.
     *
     * @param start 시작 시간 (선택)
     * @param end 종료 시간 (선택)
     * @param query 쿼리 (선택)
     * @return 레이블 목록
     */
    public Label getLabels(String start, String end, String query) {
        return lokiPort.fetchLabels(start, end, query);
    }

    /**
     * 특정 레이블의 값 목록을 조회합니다.
     *
     * @param label 레이블 이름
     * @param start 시작 시간 (선택)
     * @param end 종료 시간 (선택)
     * @param since 특정 시점 이후 (선택)
     * @param query 쿼리 (선택)
     * @return 레이블 값 목록
     */
    public Label getLabelValues(
            String label, String start, String end, String since, String query) {
        return lokiPort.fetchLabelValues(label, start, end, since, query);
    }

    /**
     * 로그 볼륨 데이터를 조회합니다.
     *
     * @param query 로그 쿼리
     * @param start 시작 시간
     * @param end 종료 시간
     * @param limit 조회 제한 수 (선택)
     * @return 로그 범위 쿼리 응답 객체
     */
    public LogVolume getLogVolumes(String query, String start, String end, Integer limit) {
        return lokiPort.fetchLogVolumes(query, start, end, limit);
    }
}
