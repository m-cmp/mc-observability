package com.mcmp.o11ymanager.manager.mapper.log;

import com.mcmp.o11ymanager.manager.dto.log.LabelResponseDto;
import com.mcmp.o11ymanager.manager.model.log.Label;


public class LabelResponseMapper {

    /**

     *
     * @param label
     * @return
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
