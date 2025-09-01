package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Grafana data source model representing data source configuration Contains data source
 * identification information including name, type, and unique identifier. Used within alert queries
 * to specify the data source for metric retrieval.
 */
@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaDataSource {
    private String name;
    private String type;
    private String uid;

    @Builder
    public GrafanaDataSource(String name, String type, String uid) {
        this.name = name;
        this.type = type;
        this.uid = uid;
    }
}
