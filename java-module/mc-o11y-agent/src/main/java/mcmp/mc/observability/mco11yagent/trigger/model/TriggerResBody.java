package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64EncodeField;
import mcmp.mc.observability.mco11yagent.monitoring.enums.ResultCode;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerResBody<T> {
    @JsonIgnore
    private ResultCode code = ResultCode.SUCCESS;
    @TriggerBase64EncodeField
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public String getRsCode() {
        return code.getCode();
    }

    public String getRsMsg() {
        return code.getMsg();
    }
}
