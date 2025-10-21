package com.mcmp.o11ymanager.manager.model.semaphore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Template {
    private Integer id;

    @JsonProperty("project_id")
    private Integer projectId;

    @JsonProperty("inventory_id")
    private Integer inventoryId;

    @JsonProperty("repository_id")
    private Integer repositoryId;

    @JsonProperty("environment_id")
    private Integer environmentId;

    @JsonProperty("view_id")
    private Integer viewId;

    private String name;
    private String playbook;
    private String arguments;
    private String description;

    @JsonProperty("allow_override_args_in_task")
    private boolean allowOverrideArgsInTask;

    @JsonProperty("suppress_success_alerts")
    private boolean suppressSuccessAlerts;

    private String app;

    @JsonProperty("git_branch")
    private String gitBranch;

    private String type;

    @JsonProperty("start_version")
    private String startVersion;

    @JsonProperty("build_template_id")
    private Integer buildTemplateId;

    private boolean autorun;

    @JsonProperty("survey_vars")
    private List<SurveyVar> surveyVars;

    private List<Vault> vaults;

    @JsonProperty("last_task")
    private Task lastTask;
}
