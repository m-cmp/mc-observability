package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.LabelResponseDto;
import com.mcmp.o11ymanager.manager.dto.log.LabelResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LabelResponseDto를 LabelResultDto로 변환하는 매퍼
 */
public class LabelResultMapper {
    private static final Logger log = LoggerFactory.getLogger(LabelResultMapper.class);

    /**
     * LabelResponseDto를 LabelsResultDto로 변환 (레이블 목록)
     */
    public static LabelResultDto.LabelsResultDto toLabelsResultDto(LabelResponseDto dto) {
        if (dto == null) {
            log.warn("LabelResponseDto가 null입니다.");
            return createEmptyLabelsResult();
        }

        try {
            List<String> labels = dto.getLabels();
            if (labels == null) {
                labels = new ArrayList<>();
            }
            
            LabelResultDto.LabelsDto labelsDto = LabelResultDto.LabelsDto.builder()
                    .labels(labels)
                    .build();
            
            return LabelResultDto.LabelsResultDto.builder()
                    .result(labelsDto)
                    .build();
        } catch (Exception e) {
            log.error("레이블 목록 변환 중 오류 발생: {}", e.getMessage(), e);
            return createEmptyLabelsResult();
        }
    }
    
    /**
     * LabelResponseDto를 LabelValuesResultDto로 변환 (레이블 값 목록)
     */
    public static LabelResultDto.LabelValuesResultDto toLabelValuesResultDto(LabelResponseDto dto) {
        if (dto == null) {
            log.warn("LabelResponseDto가 null입니다.");
            return createEmptyLabelValuesResult();
        }

        try {
            List<String> values = dto.getLabels();
            if (values == null) {
                values = new ArrayList<>();
            }
            
            // LabelValuesDto를 바로 생성하여 설정
            LabelResultDto.LabelValuesDto valuesDto = LabelResultDto.LabelValuesDto.builder()
                    .data(values)
                    .build();
            
            // LabelValuesResultDto에 LabelValuesDto를 설정
            return LabelResultDto.LabelValuesResultDto.builder()
                    .result(valuesDto)
                    .build();
        } catch (Exception e) {
            log.error("레이블 값 목록 변환 중 오류 발생: {}", e.getMessage(), e);
            return createEmptyLabelValuesResult();
        }
    }
    
    /**
     * 빈 레이블 목록 결과 생성
     */
    private static LabelResultDto.LabelsResultDto createEmptyLabelsResult() {
        LabelResultDto.LabelsDto labelsDto = LabelResultDto.LabelsDto.builder()
                .labels(Collections.emptyList())
                .build();
        
        return LabelResultDto.LabelsResultDto.builder()
                .result(labelsDto)
                .build();
    }
    
    /**
     * 빈 레이블 값 목록 결과 생성
     */
    private static LabelResultDto.LabelValuesResultDto createEmptyLabelValuesResult() {
        LabelResultDto.LabelValuesDto valuesDto = LabelResultDto.LabelValuesDto.builder()
                .data(Collections.emptyList())
                .build();
        
        return LabelResultDto.LabelValuesResultDto.builder()
                .result(valuesDto)
                .build();
    }
} 