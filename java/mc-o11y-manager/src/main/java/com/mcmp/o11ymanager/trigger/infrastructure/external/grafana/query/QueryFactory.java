package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.query;

import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;
import com.mcmp.o11ymanager.trigger.adapter.external.alert.dto.AlertRuleCreateDto;
import jakarta.annotation.PostConstruct;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory class for generating FluxQL queries for different resource types Creates InfluxDB Flux
 * queries for monitoring CPU, memory, and disk metrics. Queries are used with Grafana alert rules
 * to evaluate resource thresholds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryFactory {
    @Value("${influxdb.database}")
    private String influxDatabase;

    @Value("${influxdb.retention-policy}")
    private String influxRetentionPolicy;

    private static QueryFactory instance;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * Maps a policy aggregation name to a valid Flux function. Flux has no {@code avg} identifier —
     * the average reducer is {@code mean}. {@code min}/{@code max}/{@code last} are valid as-is.
     *
     * @param aggregation policy aggregation name (e.g. "avg", "max")
     * @return Flux aggregate function name (e.g. "mean", "max")
     */
    private static String fluxFn(String aggregation) {
        if (aggregation == null) {
            return "mean";
        }
        return "avg".equalsIgnoreCase(aggregation.trim())
                ? "mean"
                : aggregation.trim().toLowerCase();
    }

    /**
     * Generates FluxQL query string based on resource type Routes to appropriate query method based
     * on the resource type specified in the DTO.
     *
     * @param dto Alert rule creation data containing resource type and query parameters
     * @return FluxQL query string for the specified resource type
     */
    public static String fluxQL(AlertRuleCreateDto dto) {
        String tagName = dto.targetScope() + "_id";

        return switch (dto.resourceType()) {
            case CPU -> cpuQuery(dto, tagName);
            case MEMORY -> memoryQuery(dto, tagName);
            case DISK -> diskQuery(dto, tagName);
        };
    }

    /**
     * Generates FluxQL query for CPU usage monitoring Creates query with CPU-specific filters and
     * applies 100 - value transformation to convert CPU idle percentage to CPU usage percentage.
     *
     * @param dto Alert rule creation data containing query parameters
     * @param tagName Dynamic target ID field name based on target scope
     * @return FluxQL query string for CPU usage monitoring
     */
    private static String cpuQuery(AlertRuleCreateDto dto, String tagName) {
        return Flux.from("mc-observability/autogen")
                .range(-3L, -1L, ChronoUnit.MINUTES)
                .filter(
                        Restrictions.and(
                                Restrictions.measurement().equal(dto.measurement()),
                                Restrictions.field().equal(dto.field()),
                                Restrictions.tag("cpu").equal("cpu-total"),
                                Restrictions.tag(tagName).equal(dto.targetId()),
                                Restrictions.tag("ns_id").equal(dto.namespaceId())))
                .map("({ r with _value: 100.0 - r._value })")
                .aggregateWindow(1L, ChronoUnit.MINUTES, fluxFn(dto.aggregation()))
                .keep(new String[] {"_time", "_value", "ns_id", "infra_id", "node_id"})
                .groupBy(new String[] {"ns_id", "infra_id", "node_id"})
                .toString();
    }

    /**
     * Generates FluxQL query for memory usage monitoring Creates query for memory metrics with
     * standard filtering and aggregation.
     *
     * @param dto Alert rule creation data containing query parameters
     * @param tagName Dynamic target ID field name based on target scope
     * @return FluxQL query string for memory usage monitoring
     */
    private static String memoryQuery(AlertRuleCreateDto dto, String tagName) {

        return Flux.from(instance.influxDatabase + "/" + instance.influxRetentionPolicy)
                .range(-3L, -1L, ChronoUnit.MINUTES)
                .filter(
                        Restrictions.and(
                                Restrictions.measurement().equal(dto.measurement()),
                                Restrictions.field().equal(dto.field()),
                                Restrictions.tag(tagName).equal(dto.targetId()),
                                Restrictions.tag("ns_id").equal(dto.namespaceId())))
                .aggregateWindow(1L, ChronoUnit.MINUTES, fluxFn(dto.aggregation()))
                .keep(new String[] {"_time", "_value", "ns_id", "infra_id", "node_id"})
                .groupBy(new String[] {"ns_id", "infra_id", "node_id"})
                .toString();
    }

    /**
     * Generates FluxQL query for disk usage monitoring Creates query for disk metrics with standard
     * filtering and aggregation.
     *
     * @param dto Alert rule creation data containing query parameters
     * @param tagName Dynamic target ID field name based on target scope
     * @return FluxQL query string for disk usage monitoring
     */
    private static String diskQuery(AlertRuleCreateDto dto, String tagName) {
        return Flux.from(instance.influxDatabase + "/" + instance.influxRetentionPolicy)
                .range(-3L, -1L, ChronoUnit.MINUTES)
                .filter(
                        Restrictions.and(
                                Restrictions.measurement().equal(dto.measurement()),
                                Restrictions.field().equal(dto.field()),
                                Restrictions.tag(tagName).equal(dto.targetId()),
                                Restrictions.tag("ns_id").equal(dto.namespaceId())))
                .aggregateWindow(1L, ChronoUnit.MINUTES, fluxFn(dto.aggregation()))
                .keep(new String[] {"_time", "_value", "ns_id", "infra_id", "node_id"})
                .groupBy(new String[] {"ns_id", "infra_id", "node_id"})
                .toString();
    }
}
