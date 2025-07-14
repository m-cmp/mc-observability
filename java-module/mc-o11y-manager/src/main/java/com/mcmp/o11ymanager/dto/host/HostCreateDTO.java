package com.mcmp.o11ymanager.dto.host;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
@Schema(description = "호스트 생성을 위한 DTO")
public class HostCreateDTO {
    @Schema(description = "VM 또는 Hypervisor Host의 ID", example = "51cbd96c-797d-4ff1-a31b-1730ff5be0d2:94c3e402-5042-4c8e-9248-cdfcb1462deb")
    @NotBlank
    private String id;

    @Schema(description = "호스트 이름", example = "ish-agent-test")
    @NotBlank
    private String name;

    @Schema(description = "호스트 IP", example = "[\"192.168.110.28\"]")
    @NotEmpty
    private List<String> ips;

    @Schema(description = "호스트 포트", example = "22")
    @NotNull
    private int port;

    @Schema(description = "호스트 계정 이름", example = "root")
    @NotBlank
    private String user;

    @Schema(description = "호스트 계정 비밀번호", example = "mypw")
    @NotBlank
    private String password;

    @Schema(description = "호스트 설명", example = "test only host")
    private String description;

    @Schema(description = "호스트 타입", example = "vm")
    @NotBlank
    private String type;


    @Schema(description = "호스트 credeintailID", example = "dsfmeolife")
    @NotBlank
    private String credentialId;

    @Schema(description = "클라우드 서비스", example = "vmware")
    @NotBlank
    private String cloudService;



}
