package mcmp.mc.observability.agent.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;

import java.util.List;

@Setter
@Getter
public class PageableResBody<T> {
    private Long records;
    @Base64EncodeField
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<T> rows;
}
