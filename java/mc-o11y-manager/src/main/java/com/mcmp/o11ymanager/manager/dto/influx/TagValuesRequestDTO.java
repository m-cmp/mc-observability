package com.mcmp.o11ymanager.manager.dto.influx;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TagValuesRequestDTO {

    @Schema(description = "Measurement to read; omit to search every measurement", example = "cpu")
    private String measurement;

    @NotBlank @Schema(
            description = "Tag key whose values are listed",
            example = "node_id",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("tag_key")
    private String tagKey;

    @Schema(
            description = "Equality filters on other tags",
            example = "{\"ns_id\": \"ns-1\", \"infra_id\": \"infra-1\"}")
    private Map<String, String> filters;
}
