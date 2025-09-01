package com.mcmp.o11ymanager.manager.global.error;

import com.mcmp.o11ymanager.manager.dto.common.ErrorResponse;
import com.mcmp.o11ymanager.manager.enums.ResponseCode;
import com.mcmp.o11ymanager.manager.exception.agent.AgentStatusException;
import com.mcmp.o11ymanager.manager.exception.config.ConfigInitException;
import com.mcmp.o11ymanager.manager.exception.host.BadRequestException;
import com.mcmp.o11ymanager.manager.exception.host.HostAgentTaskProcessingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice(basePackages = "com.mcmp.o11ymanager.manager")
public class ManagerExceptionHandler {

  @ExceptionHandler({
      MethodArgumentTypeMismatchException.class,
      HandlerMethodValidationException.class,
      HttpMessageNotReadableException.class,
      MissingServletRequestParameterException.class,
      ConstraintViolationException.class,
      IllegalStateException.class,
      IllegalArgumentException.class,
      BadRequestException.class,
      ConfigInitException.class,
      AgentStatusException.class
  })
  protected ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
    String requestId = UUID.randomUUID().toString();
    log.error("RequestID={}, Manager BadRequest: {}", requestId, e.getMessage());
    return ErrorResponse.of(requestId, ResponseCode.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler({
      NoResourceFoundException.class,
      EntityNotFoundException.class,
      EmptyResultDataAccessException.class
  })
  protected ResponseEntity<ErrorResponse> handleNotFound(Exception e) {
    String requestId = UUID.randomUUID().toString();
    log.error("RequestID={}, Manager NotFound: {}", requestId, e.getMessage());
    return ErrorResponse.of(requestId, ResponseCode.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  protected ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
    return ErrorResponse.of(UUID.randomUUID().toString(), ResponseCode.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  protected ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
    return ErrorResponse.of(UUID.randomUUID().toString(), ResponseCode.BAD_REQUEST, "파일 크기가 제한을 초과했습니다.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  protected ResponseEntity<ErrorResponse> handleDBError(DataIntegrityViolationException e) {
    return ErrorResponse.of(UUID.randomUUID().toString(), ResponseCode.INTERNAL_SERVER_ERROR, "DB 제약조건 위반 발생");
  }

  @ExceptionHandler(HostAgentTaskProcessingException.class)
  protected ResponseEntity<ErrorResponse> handleHostAgentTask(HostAgentTaskProcessingException e) {
    return ErrorResponse.of(e.getRequestId(), ResponseCode.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
    List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
    StringBuilder parsed = new StringBuilder();
    for (ObjectError error : allErrors) {
      parsed.append(((FieldError) error).getField())
          .append("='").append(((FieldError) error).getRejectedValue()).append("', ");
    }
    return ErrorResponse.of(UUID.randomUUID().toString(), ResponseCode.BAD_REQUEST, parsed.toString());
  }

  @ExceptionHandler({Exception.class, BaseException.class})
  protected ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
    String requestId = (e instanceof BaseException be) ? be.getRequestId() : UUID.randomUUID().toString();
    log.error("RequestID={}, Manager internal error: {}", requestId, e.getMessage(), e);
    return ErrorResponse.of(requestId, ResponseCode.INTERNAL_SERVER_ERROR);
  }
}