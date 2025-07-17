package com.mcmp.o11ymanager.dto.target;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetRegisterDTO {

    @NotBlank(message = "Target ID는 필수입니다")
    private String id;

    @NotBlank(message = "Target name은 필수입니다")
    private String name;

    @JsonProperty("alias_name")
    private String aliasName;

    private String description;

    @NotBlank(message = "CSP는 필수입니다")
    private String csp;

    @NotBlank(message = "Namespace ID는 필수입니다")
    @JsonProperty("ns_id")
    private String nsId;

    @NotBlank(message = "MCI ID는 필수입니다")
    @JsonProperty("mci_id")
    private String mciId;

    @JsonProperty("vm_id")
    private String vmId;

    @JsonProperty("sub_group")
    private String subGroup;

    private String state;

    private AccessInfoDTO accessInfo;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessInfoDTO {
        private String ip;
        private Integer port;
        private String user;
        @JsonProperty("ssh_key")
        private String sshKey;
    }
}