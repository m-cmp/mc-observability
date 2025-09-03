package com.mcmp.o11ymanager.trigger.application.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the application Handles all exceptions thrown across the application
 * and provides consistent error response format.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.mcmp.o11ymanager.trigger")
public class TriggerExceptionHandler {

    @ExceptionHandler(TriggerPolicyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTriggerPolicyNotFound(
            TriggerPolicyNotFoundException e) {
        log.error("TriggerPolicy not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ErrorResponse.builder()
                                .errorCode(e.getErrorCode())
                                .message(e.getMessage())
                                .build());
    }

    @ExceptionHandler(TriggerHistoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTriggerHistoryNotFound(
            TriggerHistoryNotFoundException e) {
        log.error("TriggerHistory not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ErrorResponse.builder()
                                .errorCode(e.getErrorCode())
                                .message(e.getMessage())
                                .build());
    }

    @ExceptionHandler({
        InvalidAlertLevelException.class,
        InvalidThresholdConditionException.class,
        InvalidNotificationTypeException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e) {
        log.error("Trigger bad request: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.builder()
                                .errorCode("BAD_REQUEST")
                                .message(e.getMessage())
                                .build());
    }

    @ExceptionHandler({
        NotificationDeliveryException.class,
        NotificationConfigurationException.class,
        McO11yTriggerException.class
    })
    public ResponseEntity<ErrorResponse> handleTriggerServerError(RuntimeException e) {
        log.error("Trigger internal error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.builder()
                                .errorCode("TRIGGER_INTERNAL_ERROR")
                                .message(e.getMessage())
                                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult()
                .getAllErrors()
                .forEach(
                        error -> {
                            String fieldName = ((FieldError) error).getField();
                            String errorMessage = error.getDefaultMessage();
                            errors.put(fieldName, errorMessage);
                        });
        return ResponseEntity.badRequest()
                .body(
                        ErrorResponse.builder()
                                .errorCode("VALIDATION_FAILED")
                                .message(errors.toString())
                                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.badRequest()
                .body(
                        ErrorResponse.builder()
                                .errorCode("CONSTRAINT_VIOLATION")
                                .message(e.getMessage())
                                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("Unexpected trigger error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.builder()
                                .errorCode("INTERNAL_SERVER_ERROR")
                                .message("Unexpected error")
                                .build());
    }
}
