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
}
