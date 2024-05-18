package mcmp.mc.observability.agent.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.annotation.Base64EncodeField;
import mcmp.mc.observability.agent.enums.ResultCode;

@Setter
@Getter
public class ResBody<T> {
    @JsonIgnore
    private ResultCode code = ResultCode.SUCCESS;
    @Base64EncodeField
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public String getRsCode() {
        return code.getCode();
    }

    public String getRsMsg() {
        return code.getMsg();
    }
}
