package com.mcmp.o11ymanager.manager.infrastructure.insight;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
@Order(value = 1)
public class FeignExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignStatusException(FeignException e) {
        HttpStatus status = HttpStatus.valueOf(e.status());

        String responseBody = e.contentUTF8();

        log.warn("[FeignExceptionHandler] status={} body={}", status, responseBody);

        return ResponseEntity.status(status).body(responseBody);
    }
}
