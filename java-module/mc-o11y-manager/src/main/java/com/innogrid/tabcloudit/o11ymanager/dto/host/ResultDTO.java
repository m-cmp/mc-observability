package com.innogrid.tabcloudit.o11ymanager.dto.host;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.innogrid.tabcloudit.o11ymanager.enums.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
@Getter
@Builder
@Schema(description = "API 응답 객체")
public class ResultDTO {

  private String id;
  private ResponseStatus status;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String errorMessage;
}