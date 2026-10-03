package com.mcmp.o11ymanager.manager.infrastructure.trace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tempo /api/v2/search/tag/{tag}/values response. Values are typed; the UI only needs the text. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TempoTagValuesDto {
    private List<TagValue> tagValues;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagValue {
        private String type;
        private String value;
    }
}
