package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.model.log.Label;
import com.mcmp.o11ymanager.manager.model.log.Log;
import com.mcmp.o11ymanager.manager.model.log.LogVolume;

/** Loki 로그 저장소와의 인터페이스(포트) 도메인 계층에서 인프라 계층으로 향하는 의존성 역전을 위한 인터페이스 */
public interface LokiPort {
    /**
     * 로그 데이터를 조회합니다.
     *
     * @param query 로그 쿼리
     * @param limit 조회 제한 수
     * @return 로그 응답 객체
     */
    Log fetchLogs(String query, int limit);

    /**
     * 특정 기간의 로그 데이터를 조회합니다.
     *
     * @param query 로그 쿼리
     * @param start 시작 시간
     * @param end 종료 시간
     * @param limit 조회 제한 수
     * @param direction 조회 방향(forward/backward)
     * @param interval 개별 로그 라인(스트림) 결과 사이의 최소 시간 간격
     * @param step 메트릭 쿼리(매트릭스) 결과의 계산/평가 시간 간격
     * @param since 쿼리 시작 시점을 end 시점(또는 현재) 기준으로 기간
     * @return 로그 응답 객체
     */
    Log fetchLogs(
            String query,
            String start,
            String end,
            int limit,
            String direction,
            String interval,
            String step,
            String since);

    /**
     * 레이블 목록을 조회합니다.
     *
     * @param start 시작 시간 (선택)
     * @param end 종료 시간 (선택)
     * @param query 쿼리 (선택)
     * @return 레이블 목록
     */
    Label fetchLabels(String start, String end, String query);

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
    Label fetchLabelValues(String label, String start, String end, String since, String query);

    /**
     * 로그 볼륨 데이터를 조회합니다.
     *
     * @param query 로그 쿼리
     * @param start 시작 시간
     * @param end 종료 시간
     * @param limit 조회 제한 수 (선택)
     * @return 로그 범위 쿼리 응답 객체
     */
    LogVolume fetchLogVolumes(String query, String start, String end, Integer limit);
}
