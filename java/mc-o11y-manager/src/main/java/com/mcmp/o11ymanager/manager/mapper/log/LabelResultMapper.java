package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.LabelResponseDto;
import com.mcmp.o11ymanager.manager.dto.log.LabelResultDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabelResultMapper {

    private static final Logger log = LoggerFactory.getLogger(LabelResultMapper.class);

    public static LabelResultDto.LabelsResultDto toLabelsResultDto(LabelResponseDto dto) {
        if (dto == null) {
            log.warn("LabelResponseDto is null.");
            return createEmptyLabelsResult();
        }

        try {
            List<String> labels = dto.getLabels();
            if (labels == null) {
                labels = new ArrayList<>();
            }

            LabelResultDto.LabelsDto labelsDto =
                    LabelResultDto.LabelsDto.builder().labels(labels).build();

            return LabelResultDto.LabelsResultDto.builder().result(labelsDto).build();
        } catch (Exception e) {
            log.error("Error occurred while converting label list: {}", e.getMessage(), e);
            return createEmptyLabelsResult();
        }
    }

    public static LabelResultDto.LabelValuesResultDto toLabelValuesResultDto(LabelResponseDto dto) {
        if (dto == null) {
            log.warn("LabelResponseDto is null.");
            return createEmptyLabelValuesResult();
        }

        try {
            List<String> values = dto.getLabels();
            if (values == null) {
                values = new ArrayList<>();
            }

            LabelResultDto.LabelValuesDto valuesDto =
                    LabelResultDto.LabelValuesDto.builder().data(values).build();

            return LabelResultDto.LabelValuesResultDto.builder().result(valuesDto).build();
        } catch (Exception e) {
            log.error("Error occurred while converting label value list: {}", e.getMessage(), e);
            return createEmptyLabelValuesResult();
        }
    }

    private static LabelResultDto.LabelsResultDto createEmptyLabelsResult() {
        LabelResultDto.LabelsDto labelsDto =
                LabelResultDto.LabelsDto.builder().labels(Collections.emptyList()).build();

        return LabelResultDto.LabelsResultDto.builder().result(labelsDto).build();
    }

    private static LabelResultDto.LabelValuesResultDto createEmptyLabelValuesResult() {
        LabelResultDto.LabelValuesDto valuesDto =
                LabelResultDto.LabelValuesDto.builder().data(Collections.emptyList()).build();

        return LabelResultDto.LabelValuesResultDto.builder().result(valuesDto).build();
    }
}
