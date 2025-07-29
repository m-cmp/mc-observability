package com.mcmp.o11ymanager.dto.common;

import com.mcmp.o11ymanager.enums.ResponseCode;
import com.mcmp.o11ymanager.enums.ResponseStatus;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@SuperBuilder
public class ErrorResponse extends BaseResponse {

  private final List<ErrorDetail> errors;

  public record ErrorDetail(String field, String message) {

  }

  public static ResponseEntity<ErrorResponse> of(String requestId, ResponseCode code) {
    return error(requestId, code).toResponseEntity();
  }

  public static ResponseEntity<ErrorResponse> of(String requestId, ResponseCode code, String message) {
    return error(requestId, code, message).toResponseEntity();
  }

  public static ResponseEntity<ErrorResponse> of(String requestId, ResponseCode code, String message, List<ErrorDetail> errors) {
    return error(requestId, code, message, errors).toResponseEntity();
  }

  public static ResponseEntity<ErrorResponse> of(String requestId, ResponseCode code, String field, String errorMessage) {
    return error(requestId, code, field, errorMessage).toResponseEntity();
  }

  private static ErrorResponse error(String requestId, ResponseCode code) {
    return error(requestId, code, code.getMessage());
  }

  private static ErrorResponse error(String requestId, ResponseCode code, String message) {
    return ErrorResponse.builder()
        .timestamp(new Date())
        .status(ResponseStatus.ERROR)
        .code(code)
        .message(message)
        .requestId(requestId)
        .errors(new ArrayList<>())
        .build();
  }

  private static ErrorResponse error(String requestId, ResponseCode code, String message, List<ErrorDetail> errors) {
    return ErrorResponse.builder()
        .timestamp(new Date())
        .status(ResponseStatus.ERROR)
        .code(code)
        .message(message)
        .requestId(requestId)
        .errors(errors)
        .build();
  }

  private static ErrorResponse error(String requestId, ResponseCode code, String field, String errorMessage) {
    List<ErrorDetail> errors = new ArrayList<>();
    errors.add(new ErrorDetail(field, errorMessage));

    return ErrorResponse.builder()
        .timestamp(new Date())
        .status(ResponseStatus.ERROR)
        .code(code)
        .message(code.getMessage())
        .requestId(requestId)
        .errors(errors)
        .build();
  }
} 