package com.mcmp.o11ymanager.mapper.history;

import com.mcmp.o11ymanager.dto.history.HistoryResponseDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.entity.HistoryEntity;
import com.mcmp.o11ymanager.global.definition.TimestampDefinition;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class HistoryMapper {
    public HistoryResponseDTO toDTO(HistoryEntity history, TargetDTO targetDTO) {
        return HistoryResponseDTO.builder()
                .id(history.getId())
                .timestamp(history.getTimestamp() != null ? history.getTimestamp().format(DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)) : null)
                .requestUserId(history.getRequestUserId())
                .isSuccess(history.isSuccess())
                .action(history.getAgentAction())
                .hostResponseDTO(targetDTO)
                .build();
    }

}
