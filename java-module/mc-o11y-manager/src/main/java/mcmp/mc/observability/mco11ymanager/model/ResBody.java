package mcmp.mc.observability.mco11ymanager.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResBody<T> {
    private String rsCode;
    private String rsMsg;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
    private String errorMessage;
}
