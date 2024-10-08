package mcmp.mc.observability.mco11yagent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64EncodeField;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResBody<T> {

    @Builder.Default
    @JsonIgnore
    @JsonProperty("code")
    private ResultCode code = ResultCode.SUCCESS;

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
}
