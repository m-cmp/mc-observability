package com.mcmp.o11ymanager.manager.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Response for cb-spider {@code GET /spider/cluster?ConnectionName=...}. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpiderClusterList {
    private List<SpiderClusterInfo> cluster;
}
