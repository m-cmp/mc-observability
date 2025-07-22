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

    @NotBlank(message = "Target name은 필수입니다")
    private String name;

    @JsonProperty("alias_name")
    private String aliasName;

    private String description;
}