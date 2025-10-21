package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Label result response DTO */
public class LabelResultDto {

    /** Label list query result */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelsResultDto {
        private LabelsDto result;
    }

    /** Label value list query result */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelValuesResultDto {
        private LabelValuesDto result;
    }

    /** Label list data */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelsDto {
        private List<String> labels;
    }

    /** Label value list data */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelValuesDto {
        private List<String> data;
    }
}
