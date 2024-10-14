package mcmp.mc.observability.mco11yagent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64EncodeField;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class ResBody<T> {
    @JsonIgnore
    private ResultCode code;

    @Base64EncodeField
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("data")
    private T data;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("rs_code")
    private String rsCode;

    @JsonProperty("rs_msg")
    private String rsMsg;

    public ResBody() {
        this.setCode(ResultCode.SUCCESS);
    }

    public ResBody(ResultCode code) {
        this.setCode(code);
    }

    public ResBody(ResultCode code, T data) {
        this.setCode(code);
        this.data = data;
    }

    public void setCode(ResultCode code) {
        this.code = code;
        this.rsCode = code.getCode();
        this.rsMsg = code.getMsg();
        this.errorMessage = "";
    }
}
