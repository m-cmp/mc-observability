package com.mcmp.o11ymanager.manager.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseCode {
  // Success Codes (2xx)
  OK(HttpStatus.OK, "Processed successfully."),
  ACCEPTED(HttpStatus.ACCEPTED, "Request has been accepted."),

  // Client Error Codes (4xx)
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Access is denied."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed."),

  // Server Error Codes (5xx)
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred."),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Service is unavailable."),
  VM_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
      "Unable to connect to the virtual machine."),
  TELEGRAF_CONFIG_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Telegraf configuration not found.");


  private final HttpStatus httpStatus;
  private final String message;

  ResponseCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }

  public int getCode() {
    return httpStatus.value();
  }
}
