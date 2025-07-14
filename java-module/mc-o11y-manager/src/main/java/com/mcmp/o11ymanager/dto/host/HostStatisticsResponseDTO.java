package com.mcmp.o11ymanager.dto.host;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "호스트 정보 반환 DTO")
public class HostStatisticsResponseDTO {

    @Schema(description = "등록된 총 호스트 갯수", example = "10")
    private Integer registeredTotal;

    @Schema(description = "동작 중인 호스트 갯수", example = "8")
    private Integer running;

    @Schema(description = "동작 중이지 않은 호스트 갯수", example = "2")
    private Integer failed;

    @Schema(description = "모니터링 에이전트가 설치된 호스트 갯수", example = "8")
    private Integer monitoringAgentInstalled;

    @Schema(description = "모니터링 에이전트가 설치되지 않은 호스트 갯수", example = "2")
    private Integer monitoringAgentNotInstalled;

    @Schema(description = "로그 에이전트가 설치된 호스트 갯수", example = "8")
    private Integer logAgentInstalled;

    @Schema(description = "로그 에이전트가 설치되지 않은 호스트 갯수", example = "2")
    private Integer logAgentNotInstalled;
}
