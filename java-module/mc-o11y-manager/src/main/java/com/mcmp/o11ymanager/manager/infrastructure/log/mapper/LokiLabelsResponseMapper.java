package com.mcmp.o11ymanager.manager.infrastructure.log.mapper;

import com.mcmp.o11ymanager.manager.infrastructure.log.dto.LokiLabelsResponseDto;
import com.mcmp.o11ymanager.manager.model.log.Label;
import java.util.Collections;

public class LokiLabelsResponseMapper {

    public static Label toDomain(LokiLabelsResponseDto dto) {
        if (dto == null) {
            return Label.builder().status("failure").labels(Collections.emptyList()).build();
        }

        return Label.builder().status(dto.getStatus()).labels(dto.getData()).build();
    }
}
