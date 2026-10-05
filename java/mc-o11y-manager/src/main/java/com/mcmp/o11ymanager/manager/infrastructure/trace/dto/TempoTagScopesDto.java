package com.mcmp.o11ymanager.manager.infrastructure.trace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tempo /api/v2/search/tags response: attribute names grouped by scope (resource, span, ...). */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TempoTagScopesDto {
    private List<Scope> scopes;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Scope {
        private String name;
        private List<String> tags;
    }
}
