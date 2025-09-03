package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.LabelResponseDto;
import com.mcmp.o11ymanager.manager.model.log.Label;

/** 레이블 도메인 모델을 응용 계층 DTO로 변환하는 매퍼 */
public class LabelResponseMapper {

    /**
     * 도메인 모델을 DTO로 변환
     *
     * @param label 레이블 도메인 모델
     * @return 응용 계층 DTO
     */
    public static LabelResponseDto toDto(Label label) {
        if (label == null) {
            return null;
        }

        return LabelResponseDto.builder()
                .status(label.getStatus())
                .labels(label.getLabels())
                .build();
    }
}
