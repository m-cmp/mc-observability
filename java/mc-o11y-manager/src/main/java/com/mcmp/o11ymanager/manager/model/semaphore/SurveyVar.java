package com.mcmp.o11ymanager.manager.model.semaphore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SurveyVar {
    private String name;
    private String title;
    private String description;
    private String type;
    private boolean required;
    private List<SurveyValue> values;
}
