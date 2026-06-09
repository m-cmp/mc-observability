package com.mcmp.o11ymanager.manager.infrastructure.trace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tempo /api/search/tag/service.name/values response (distinct service.name values). */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TempoServiceValuesDto {
    private List<String> tagValues;
}
