package com.mcmp.o11ymanager.manager.global.vm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.mcmp.o11ymanager.manager.enums.ResponseCode;
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

    @JsonProperty("rs_code")
    private String rsCode = "0000";

    @JsonProperty("rs_msg")
    private String rsMsg = "완료되었습니다.";

    private T data;

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
