package mcmp.mc.observability.agent.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;
import mcmp.mc.observability.agent.monitoring.enums.ResultCode;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
