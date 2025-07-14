package com.mcmp.o11ymanager.dto.history;

import com.mcmp.o11ymanager.entity.HistoryEntity;
import com.mcmp.o11ymanager.enums.AgentAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryDTO {
    private String id;
    private String hostId;
    private LocalDateTime timestamp;
    private AgentAction agentAction;
    private String requestUserId;
    private boolean isSuccess;
    private String reason;

    // HistoryEntity와 DTO 간의 변환 메서드
    public static HistoryDTO fromEntity(HistoryEntity entity) {
        return HistoryDTO.builder()
                .id(entity.getId())
                .hostId(entity.getHostId())
                .timestamp(entity.getTimestamp())
                .agentAction(entity.getAgentAction())
                .requestUserId(entity.getRequestUserId())
                .isSuccess(entity.isSuccess())
                .reason(entity.getReason())
                .build();
    }

    public HistoryEntity toEntity() {
        return HistoryEntity.builder()
                .id(this.id)
                .hostId(this.hostId)
                .timestamp(this.timestamp)
                .agentAction(this.agentAction)
                .requestUserId(this.requestUserId)
                .isSuccess(this.isSuccess)
                .reason(this.reason)
                .build();
    }
}