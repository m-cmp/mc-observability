package com.mcmp.o11ymanager.manager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Response body for {@code GET /tumblebug/ns/{nsId}/infra} (list all Infras in a namespace). */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TumblebugInfraList {

    @JsonProperty("infra")
    private List<TumblebugInfra> infra;
}
