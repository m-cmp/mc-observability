package com.mcmp.o11ymanager.dto.host;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Agent 관련 작업 요청을 위한 DTO")
public class AgentDTO {
    @Schema(description = "호스트 ID 리스트", example = "[\"51cbd96c-797d-4ff1-a31b-1730ff5be0d2\", \"94c3e402-5042-4c8e-9248-cdfcb1462deb\"]")
    @NotBlank
    private String[] host_id_list;

    @Schema(description = "모니터링 에이전트 선택", example = "true", defaultValue = "false")
    @NotBlank
    private boolean selectMonitoringAgent;

    @Schema(description = "로그 에이전트 선택", example = "true", defaultValue = "false")
    @NotBlank
    private boolean selectLogAgent;

    public AgentDTO(boolean isSelectMonitoringAgent, boolean isSelectLogAgent, String[] host_id_list) {
        this.selectMonitoringAgent = isSelectMonitoringAgent;
        this.selectLogAgent = isSelectLogAgent;
        this.host_id_list = host_id_list;
    }
}
