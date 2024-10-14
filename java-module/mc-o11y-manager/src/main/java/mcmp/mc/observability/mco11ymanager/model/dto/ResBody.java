package mcmp.mc.observability.mco11ymanager.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import mcmp.mc.observability.mco11ymanager.annotation.Base64EncodeField;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResBody<T> {
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
