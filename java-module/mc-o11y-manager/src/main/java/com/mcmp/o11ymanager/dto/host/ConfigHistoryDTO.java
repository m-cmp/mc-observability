package com.mcmp.o11ymanager.dto.host;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "config 설정을 위한 DTO")
public class ConfigHistoryDTO {

    @Schema(description = "git 커밋 해시 값", example = "4592b9f9b55a3b922dd03a9dd72b6a676bf44cac")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String commitHash;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String message;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String timestamp;
}
