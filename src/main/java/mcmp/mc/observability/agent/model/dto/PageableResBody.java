package mcmp.mc.observability.agent.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PageableResBody<T> {
    private Long records;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T rows;
}
