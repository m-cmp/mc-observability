package com.innogrid.tabcloudit.o11ymanager.dto.host;

import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "호스트 정보 반환 DTO")
public class HostResponseDTO {

    @Schema(description = "VM 또는 Hypervisor Host의 ID", example = "51cbd96c-797d-4ff1-a31b-1730ff5be0d2:94c3e402-5042-4c8e-9248-cdfcb1462deb")
    private final String id;

    @Schema(description = "", example = "ish-agent-test.novalocal")
    private final String hostname;

    @Schema(description = "호스트 이름", example = "ish-agent-test")
    private String name;

    @Schema(description = "호스트 타입", example = "vm")
    private String type;

    @Schema(description = "호스트 IP", example = "192.168.110.28")
    private String ip;

    @Schema(description = "호스트 포트", example = "22")
    private int port;

    @Schema(description = "호스트 계정 이름", example = "root")
    private String user;

    @Schema(description = "호스트 설명", example = "test only host")
    private String description;

    @Schema(description = "호스트 상태", example = "RUNNING")
    private HostStatus hostStatus;

    @Schema(description = "Telegraf 상태", example = "ACTIVE")
    private String monitoringServiceStatus;

    @Schema(description = "Fluent-Bit 상태", example = "ACTIVE")
    private String logServiceStatus;

    @Schema(description = "모니터링 에이전트 config git 커밋 해시 값", example = "4592b9f9b55a3b922dd03a9dd72b6a676bf44cac")
    private final String monitoringAgentConfigGitHash;

    @Schema(description = "로그 에이전트 config git 커밋 해시 값", example = "4592b9f9b55a3b922dd03a9dd72b6a676bf44cac")
    private final String logAgentConfigGitHash;

    @Schema(description = "모니터링 에이전트 버전", example = "v1.0.0_250312-4ccbdfb")
    private String monitoringAgentVersion;

    @Schema(description = "로그 에이전트 버전", example = "v1.0.0_250312-4ccbdfb")
    private String logAgentVersion;

    @Schema(description = "모니터링 설치 상태", example = "IDLE")
    private HostAgentTaskStatus hostMonitoringAgentTaskStatus;

    @Schema(description = "로그 설치 상태", example = "IDLE")
    private HostAgentTaskStatus hostLogAgentTaskStatus;

    @Schema(description = "", example = "2025-03-07T10:44:25")
    private final String createdAt;

    @Schema(description = "", example = "2025-03-07T12:41:41")
    private String updatedAt;

    @Schema(description = "호스트 credeintailID", example = "dsfmeolife")
    @NotBlank
    private String credentialId;

    @Schema(description = "클라우드 서비스", example = "vmware")
    @NotBlank
    private String cloudService;
}
