package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 레이블 결과 응답 DTO */
public class LabelResultDto {

    /** 레이블 목록 조회 결과 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelsResultDto {
        private LabelsDto result;
    }

    /** 레이블 값 목록 조회 결과 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelValuesResultDto {
        private LabelValuesDto result;
    }

    /** 레이블 목록 데이터 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelsDto {
        private List<String> labels;
    }

    /** 레이블 값 목록 데이터 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelValuesDto {
        private List<String> data;
    }
}
