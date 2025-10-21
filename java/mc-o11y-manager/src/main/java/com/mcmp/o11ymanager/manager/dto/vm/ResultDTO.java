package com.mcmp.o11ymanager.manager.dto.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "API response object")
public class ResultDTO {

    private String nsId;
    private String mciId;
    private String vmId;
    private ResponseStatus status;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String errorMessage;
}
