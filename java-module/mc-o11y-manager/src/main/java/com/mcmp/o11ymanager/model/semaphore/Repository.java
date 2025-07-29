package com.mcmp.o11ymanager.model.semaphore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Repository {
    private Integer id;
    private String name;
    @JsonProperty("project_id")
    private Integer projectId;
    @JsonProperty("git_url")
    private String gitUrl;
    @JsonProperty("git_branch")
    private String gitBranch;
    @JsonProperty("ssh_key_id")
    private Integer sshKeyId;
}
