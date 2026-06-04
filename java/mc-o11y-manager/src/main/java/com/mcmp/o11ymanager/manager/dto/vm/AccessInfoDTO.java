package com.mcmp.o11ymanager.manager.dto.vm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessInfoDTO {
    private String ip;
    private Integer port;
    private String user;

    @JsonProperty("ssh_key")
    private String sshKey;

    // Windows VM의 administrator password. Tumblebug `vmUserPassword`에서 가져옴.
    // Linux VM은 SSH key 인증이라 null. Ansible winrm 인증 시 `target_password` env로 전달된다.
    private String password;

    // "linux" | "windows". Ansible playbook이 이 값을 보고 agent vs agent-windows role을 분기.
    // null이면 SemaphoreDomainService에서 "linux"로 기본 처리(Telegraf/FluentBit/Beyla 기존 호출부 영향 없음).
    @Builder.Default private String osType = "linux";

    // Windows winrm scheme: "http" (5985) | "https" (5986). Ansible task의 ansible_winrm_scheme로 전달.
    // Linux는 null (사용 안 함).
    private String winrmScheme;
}
