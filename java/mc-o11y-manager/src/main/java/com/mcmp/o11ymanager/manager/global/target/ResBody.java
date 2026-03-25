package com.mcmp.o11ymanager.manager.global.vm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mcmp.o11ymanager.manager.enums.ResponseCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"rs_code", "rs_msg", "data", "error_message"})
public class ResBody<T> {

    @Schema(description = "Response code", example = "0000")
    @JsonProperty("rs_code")
    private String rsCode = "0000";

    @Schema(description = "Response message", example = "success")
    @JsonProperty("rs_msg")
    private String rsMsg = "success";

    private T data;

    @Schema(description = "Error message", example = "")
    @JsonProperty("error_message")
    private String errorMessage = "";

    public ResBody(T data) {
        this.data = data;
    }

    public ResBody(String rsCode, String rsMsg, String errorMessage) {
        this.rsCode = rsCode;
        this.rsMsg = rsMsg;
        this.errorMessage = errorMessage;
    }

    public static <T> ResBody<T> error(ResponseCode responseCode, String errorMessage) {
        return new ResBody<>(
                String.valueOf(responseCode.getCode()), responseCode.getMessage(), errorMessage);
    }

    public static <T> ResBody<T> success(T data) {
        return new ResBody<>(data);
    }
}
