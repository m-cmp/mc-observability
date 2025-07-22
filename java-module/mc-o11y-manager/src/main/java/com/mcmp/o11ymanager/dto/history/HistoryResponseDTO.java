package com.mcmp.o11ymanager.dto.history;

import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.enums.AgentAction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@Builder
@RequiredArgsConstructor
@Schema(description = "이력 조회 DTO")
public class HistoryResponseDTO {

    private final String id;

    @Schema(description = "기록 시간", example = "ACTIVE")
    private final String timestamp;

    @Schema(description = "Agent 행위", example = "INSTALL")
    private final AgentAction action;

    @Schema(description = "Agent 행위 결과", example = "true")
    private final boolean isSuccess;

    @Schema(description = "요청 유저", example = "admin")
    private final String requestUserId;

    @Deprecated
    @Schema(description = "타겟 정보(deprecated)")
    private final TargetDTO targetDTO;

}
