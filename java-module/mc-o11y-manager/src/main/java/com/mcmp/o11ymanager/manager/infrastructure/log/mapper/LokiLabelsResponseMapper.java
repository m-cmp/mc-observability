package com.mcmp.o11ymanager.manager.infrastructure.log.mapper;

import com.mcmp.o11ymanager.manager.infrastructure.log.dto.LokiLabelsResponseDto;
import com.mcmp.o11ymanager.manager.model.log.Label;
import java.util.Collections;

/** Loki API의 레이블 응답 DTO를 도메인 모델로 변환하는 매퍼 */
public class LokiLabelsResponseMapper {

    /** DTO를 도메인 모델로 변환 */
    public static Label toDomain(LokiLabelsResponseDto dto) {
        if (dto == null) {
            return Label.builder().status("failure").labels(Collections.emptyList()).build();
        }

        return Label.builder().status(dto.getStatus()).labels(dto.getData()).build();
    }
}
