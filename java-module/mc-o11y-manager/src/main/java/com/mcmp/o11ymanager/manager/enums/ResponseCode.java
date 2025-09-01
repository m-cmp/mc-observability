package com.mcmp.o11ymanager.manager.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseCode {
    // Success Codes (2xx)
    OK(HttpStatus.OK, "정상적으로 처리되었습니다."),    
    ACCEPTED(HttpStatus.ACCEPTED, "요청이 접수되었습니다."),    

    // Client Error Codes (4xx)
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),    

    // Server Error Codes (5xx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스를 사용할 수 없습니다."),
    VM_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "가상머신에 연결할 수 없습니다."),
    TELEGRAF_CONFIG_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Telegraf 설정을 찾을 수 없습니다.");

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