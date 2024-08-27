package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64EncodeField;

import java.util.List;

@Setter
@Getter
public class PageableResBody<T> {
    private Long records;
    @TriggerBase64EncodeField
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<T> rows;
}
