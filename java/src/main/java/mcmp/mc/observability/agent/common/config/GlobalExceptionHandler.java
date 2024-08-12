package mcmp.mc.observability.agent.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.agent.common.exception.ResultCodeException;
import mcmp.mc.observability.agent.common.model.ResBody;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = {ResultCodeException.class})
    protected ResBody<?> resultCodeException(Exception exception) {
        ResultCodeException e = (ResultCodeException)exception;
        log.error("ResultCodeException throw Exception message : " + e.getMessage(), e.getObjects());
        log.error("ResultCodeException throw Exception stacktrace : {}", ExceptionUtils.getStackTrace(e));
        return ResBody.builder().code(e.getResultCode()).build();
    }
}