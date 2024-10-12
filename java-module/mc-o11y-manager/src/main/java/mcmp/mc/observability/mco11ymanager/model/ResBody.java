package mcmp.mc.observability.mco11ymanager.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Generic response body wrapper")
public class ResBody<T> {

    @JsonProperty("rs_code")
    @Schema(description = "Response code")
    private String rsCode;

    @JsonProperty("rs_msg")
    @Schema(description = "Response message")
    private String rsMsg;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("data")
    @Schema(description = "Response data")
    T data;

    @JsonProperty("error_message")
    @Schema(description = "Error message")
    private String errorMessage;
}
