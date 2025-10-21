package com.mcmp.o11ymanager.manager.model.semaphore;

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
public class Project {
    private Integer id;
    private String name;
    private String created;
    private boolean alert;

    @JsonProperty("alert_chat")
    private String alertChat;

    @JsonProperty("max_parallel_tasks")
    private Integer maxParallelTasks;

    private String type;
}
