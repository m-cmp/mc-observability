package com.innogrid.tabcloudit.o11ymanager.global.error;

import com.innogrid.tabcloudit.o11ymanager.dto.common.ErrorResponse;
import com.innogrid.tabcloudit.o11ymanager.enums.ResponseCode;
import com.innogrid.tabcloudit.o11ymanager.exception.agent.AgentStatusException;
import com.innogrid.tabcloudit.o11ymanager.exception.config.ConfigInitException;
import com.innogrid.tabcloudit.o11ymanager.exception.host.*;
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
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 400 Bad Request Errors
  @ExceptionHandler({
      MethodArgumentTypeMismatchException.class,
      HandlerMethodValidationException.class,
      HttpMessageNotReadableException.class,
      MissingServletRequestParameterException.class,
      ConstraintViolationException.class,
      IllegalStateException.class,
      IllegalArgumentException.class,
      AgentFailureException.class,
      BadRequestException.class,
      ConfigInitException.class,
      AgentStatusException.class,
  })
  protected ResponseEntity<ErrorResponse> handleBadRequestException(Exception e) {
    String requestId = UUID.randomUUID().toString();

    e.getCause();

    log.error("RequestID: {}, Bad Request Exception: {}", requestId, e.getMessage());
    return ErrorResponse.of(requestId, ResponseCode.BAD_REQUEST, e.getMessage());
  }

  // 404 Not Found Errors
  @ExceptionHandler({
      NoResourceFoundException.class,
      EntityNotFoundException.class,
      EmptyResultDataAccessException.class
  })
  protected ResponseEntity<ErrorResponse> handleNotFoundException(Exception e) {
    String requestId = UUID.randomUUID().toString();
    log.error("RequestID: {}, Not Found Exception: {}", requestId, e.getMessage());
    if (e instanceof NoResourceFoundException) {
      return ErrorResponse.of(requestId, ResponseCode.NOT_FOUND, e.getMessage());
    }
    return ErrorResponse.of(requestId, ResponseCode.NOT_FOUND);
  }

  // 405 Method Not Allowed
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  protected ResponseEntity<ErrorResponse> handleMethodNotAllowedException(
      HttpRequestMethodNotSupportedException e) {
    String requestId = UUID.randomUUID().toString();
    log.error("RequestID: {}, Method Not Allowed Exception: {}", requestId, e.getMessage());
    return ErrorResponse.of(requestId, ResponseCode.METHOD_NOT_ALLOWED);
  }

  // 413 Payload Too Large
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  protected ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException e) {
    String requestId = UUID.randomUUID().toString();
    log.error("RequestID: {}, File Size Exceeded Exception: {}", requestId, e.getMessage());
    return ErrorResponse.of(requestId, ResponseCode.BAD_REQUEST, "파일 크기가 제한을 초과했습니다.");
  }

  // Database Related Errors (500)
  @ExceptionHandler(DataIntegrityViolationException.class)
  protected ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException e) {
    String requestId = UUID.randomUUID().toString();
    log.error("RequestID: {}, Database Error: {}", requestId, e.getMessage());
    return ErrorResponse.of(requestId, ResponseCode.INTERNAL_SERVER_ERROR,
        "데이터베이스 제약조건 위반이 발생했습니다.");
  }

  // Generic Server Error (500)
  @ExceptionHandler({
      Exception.class,
      BaseException.class,
  })
  protected ResponseEntity<ErrorResponse> handleException(Exception e) {
    String requestId;
    if (e instanceof BaseException baseException) {
      requestId = baseException.getRequestId();
    } else {
      requestId = UUID.randomUUID().toString();
    }
    log.error("RequestID: {}, Internal Server Error: {}", requestId, e.getMessage());
    for (StackTraceElement stackTraceElement : e.getStackTrace()) {
      log.error(stackTraceElement.toString());
    }

    return ErrorResponse.of(requestId, ResponseCode.INTERNAL_SERVER_ERROR);
  }



  @ExceptionHandler(HostAgentTaskProcessingException.class)
  protected ResponseEntity<ErrorResponse> handleHostAgentTaskProcessingException(
      HostAgentTaskProcessingException e) {
    String requestId = e.getRequestId();
    log.error("RequestID: {}, Host Agent Task Processing Exception: {}", requestId, e.getMessage());
    return ErrorResponse.of(e.getRequestId(), ResponseCode.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleException(MethodArgumentNotValidException e) {
    String requestId = UUID.randomUUID().toString();
    List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
    String parsedMessage = getParsedMethodArgumentNotValidExceptionMessage(allErrors.iterator());

    log.error("RequestID: {}, Method Argument Not Valid Exception: {}", requestId, e.getMessage());
    return ErrorResponse.of(UUID.randomUUID().toString(), ResponseCode.BAD_REQUEST, parsedMessage);
  }

  private String getParsedMethodArgumentNotValidExceptionMessage(
      Iterator<ObjectError> errorIterator) {
    final StringBuilder resultMessageBuilder = new StringBuilder();
    while (errorIterator.hasNext()) {
      ObjectError error = errorIterator.next();
      resultMessageBuilder
          .append(((FieldError) error).getField())
          .append("' is '")
          .append(((FieldError) error).getRejectedValue());

      if (errorIterator.hasNext()) {
        resultMessageBuilder.append(", ");
      }
    }

    log.error(resultMessageBuilder.toString());
    return resultMessageBuilder.toString();
  }


}
