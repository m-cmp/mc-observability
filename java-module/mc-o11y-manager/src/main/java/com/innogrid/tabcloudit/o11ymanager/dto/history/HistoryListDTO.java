package com.innogrid.tabcloudit.o11ymanager.dto.history;

import com.innogrid.tabcloudit.o11ymanager.enums.AgentAction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
@Builder
public class HistoryListDTO {
    private final String id;

    @Schema(description = "기록 시간", example = "ACTIVE")
    private final String timestamp;

    @Schema(description = "Agent 행위", example = "INSTALL")
    private final AgentAction action;

    @Schema(description = "Agent 행위 결과", example = "true")
    private final boolean isSuccess;

    @Schema(description = "요청 유저", example = "admin")
    private final String requestUserId;
}
