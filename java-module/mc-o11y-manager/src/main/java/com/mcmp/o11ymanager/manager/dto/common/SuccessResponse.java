package com.mcmp.o11ymanager.manager.dto.common;

import com.mcmp.o11ymanager.manager.enums.ResponseCode;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import java.util.Date;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.ResponseEntity;

@Getter
@SuperBuilder
public class SuccessResponse<T> extends BaseResponse {

    private final DataWrapper<T> data;

    public record DataWrapper<T>(T result) {}

    public static ResponseEntity<SuccessResponse<Void>> of(String requestId) {
        return success(requestId).toResponseEntity();
    }

    public static ResponseEntity<SuccessResponse<Void>> of(String requestId, ResponseCode code) {
        return success(requestId, code).toResponseEntity();
    }

    public static ResponseEntity<SuccessResponse<Void>> of(
            String requestId, ResponseCode code, String message) {
        return success(requestId, code, message).toResponseEntity();
    }

    public static <T> ResponseEntity<SuccessResponse<T>> of(String requestId, T data) {
        return success(requestId, data).toResponseEntity();
    }

    public static <T> ResponseEntity<SuccessResponse<T>> of(
            String requestId, T data, ResponseCode code) {
        return success(requestId, data, code).toResponseEntity();
    }

    public static <T> ResponseEntity<SuccessResponse<T>> of(
            String requestId, T data, ResponseCode code, String message) {
        return success(requestId, data, code, message).toResponseEntity();
    }

    private static SuccessResponse<Void> success(String requestId) {
        return success(requestId, ResponseCode.OK);
    }

    private static SuccessResponse<Void> success(String requestId, ResponseCode code) {
        return success(requestId, code, code.getMessage());
    }

    private static SuccessResponse<Void> success(
            String requestId, ResponseCode code, String message) {
        return SuccessResponse.<Void>builder()
                .timestamp(new Date())
                .status(ResponseStatus.SUCCESS)
                .code(code)
                .message(message)
                .requestId(requestId)
                .data(new DataWrapper<>(null))
                .build();
    }

    private static <T> SuccessResponse<T> success(String requestId, T data) {
        return success(requestId, data, ResponseCode.OK);
    }

    private static <T> SuccessResponse<T> success(String requestId, T data, ResponseCode code) {
        return success(requestId, data, code, code.getMessage());
    }

    private static <T> SuccessResponse<T> success(
            String requestId, T data, ResponseCode code, String message) {
        return SuccessResponse.<T>builder()
                .timestamp(new Date())
                .status(ResponseStatus.SUCCESS)
                .code(code)
                .message(message)
                .requestId(requestId)
                .data(new DataWrapper<>(data))
                .build();
    }
}
