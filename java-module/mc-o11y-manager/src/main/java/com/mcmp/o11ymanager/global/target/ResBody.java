package com.mcmp.o11ymanager.global.target;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}