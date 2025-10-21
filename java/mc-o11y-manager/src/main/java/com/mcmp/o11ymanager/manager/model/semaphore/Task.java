package com.mcmp.o11ymanager.manager.model.semaphore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {
    private Integer id;

    @JsonProperty("template_id")
    private Integer templateId;

    private String status;
    private boolean debug;
    private String playbook;

    private String environment;

    private String secret;
    private String limit;

    @JsonProperty("git_branch")
    private String gitBranch;

    public Task setEnvironmentString(Environment env) throws JsonProcessingException {
        if (env != null) {
            this.environment = env.toJson();
        }

        return this;
    }
}
