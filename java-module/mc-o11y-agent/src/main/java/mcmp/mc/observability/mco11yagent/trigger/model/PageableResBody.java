package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64EncodeField;

import java.util.List;

@Setter
@Getter
public class PageableResBody<T> {
    @JsonProperty("records")
    private Long records;

    @TriggerBase64EncodeField
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("rows")
    private List<T> rows;
}
