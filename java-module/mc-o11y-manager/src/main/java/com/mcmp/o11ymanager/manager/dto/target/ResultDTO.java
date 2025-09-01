package com.mcmp.o11ymanager.manager.dto.target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "API 응답 객체")
public class ResultDTO {

  private String nsId;
  private String mciId;
  private String targetId;
  private ResponseStatus status;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String errorMessage;
}