package com.mcmp.o11ymanager.manager.model.semaphore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Inventory {
    private Integer id;
    private String name;
    @JsonProperty("ssh_key_id")
    private Integer sshKeyId;
    @JsonProperty("become_key_id")
    private Integer becomeKeyId;
    @JsonProperty("repository_id")
    private Integer repositoryId;
    private String type;
    private String inventory;
    @JsonProperty("project_id")
    private Integer projectId;
}
