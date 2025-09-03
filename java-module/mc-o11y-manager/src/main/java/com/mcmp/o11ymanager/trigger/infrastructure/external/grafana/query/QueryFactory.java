package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.query;

import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;
import com.mcmp.o11ymanager.trigger.adapter.external.alert.dto.AlertRuleCreateDto;
import java.time.temporal.ChronoUnit;

/**
 * Factory class for generating FluxQL queries for different resource types Creates InfluxDB Flux
 * queries for monitoring CPU, memory, and disk metrics. Queries are used with Grafana alert rules
 * to evaluate resource thresholds.
 */
public class QueryFactory {
    /**
     * Generates FluxQL query string based on resource type Routes to appropriate query method based
     * on the resource type specified in the DTO.
     *
     * @param dto Alert rule creation data containing resource type and query parameters
     * @return FluxQL query string for the specified resource type
     */
    public static String fluxQL(AlertRuleCreateDto dto) {
        String targetIdName = dto.targetScope() + "_id";

        return switch (dto.resourceType()) {
            case CPU -> cpuQuery(dto, targetIdName);
            case MEMORY -> memoryQuery(dto, targetIdName);
            case DISK -> diskQuery(dto, targetIdName);
        };
    }

    /**
     * Generates FluxQL query for CPU usage monitoring Creates query with CPU-specific filters and
     * applies 100 - value transformation to convert CPU idle percentage to CPU usage percentage.
     *
     * @param dto Alert rule creation data containing query parameters
     * @param targetIdName Dynamic target ID field name based on target scope
     * @return FluxQL query string for CPU usage monitoring
     */
    private static String cpuQuery(AlertRuleCreateDto dto, String targetIdName) {
        return Flux.from("mc-observability/autogen")
                .range(-3L, -1L, ChronoUnit.MINUTES)
                .filter(
                        Restrictions.and(
                                Restrictions.measurement().equal(dto.measurement()),
                                Restrictions.field().equal(dto.field()),
                                Restrictions.tag("cpu").equal("cpu-total"),
                                Restrictions.tag(targetIdName).equal(dto.targetId()),
                                Restrictions.tag("ns_id").equal(dto.namespaceId())))
                .map("({ r with _value: 100.0 - r._value })")
                .aggregateWindow(1L, ChronoUnit.MINUTES, dto.aggregation())
                .keep(new String[] {"_time", "_value", "ns_id", "mci_id", "vm_id"})
                .groupBy(new String[] {"ns_id", "mci_id", "vm_id"})
                .toString();
    }

    /**
     * Generates FluxQL query for memory usage monitoring Creates query for memory metrics with
     * standard filtering and aggregation.
     *
     * @param dto Alert rule creation data containing query parameters
     * @param targetIdName Dynamic target ID field name based on target scope
     * @return FluxQL query string for memory usage monitoring
     */
    private static String memoryQuery(AlertRuleCreateDto dto, String targetIdName) {
        return Flux.from("mc-observability/autogen")
                .range(-3L, -1L, ChronoUnit.MINUTES)
                .filter(
                        Restrictions.and(
                                Restrictions.measurement().equal(dto.measurement()),
                                Restrictions.field().equal(dto.field()),
                                Restrictions.tag(targetIdName).equal(dto.targetId()),
                                Restrictions.tag("ns_id").equal(dto.namespaceId())))
                .aggregateWindow(1L, ChronoUnit.MINUTES, dto.aggregation())
                .keep(new String[] {"_time", "_value", "ns_id", "mci_id", "vm_id"})
                .groupBy(new String[] {"ns_id", "mci_id", "vm_id"})
                .toString();
    }

    /**
     * Generates FluxQL query for disk usage monitoring Creates query for disk metrics with standard
     * filtering and aggregation.
     *
     * @param dto Alert rule creation data containing query parameters
     * @param targetIdName Dynamic target ID field name based on target scope
     * @return FluxQL query string for disk usage monitoring
     */
    private static String diskQuery(AlertRuleCreateDto dto, String targetIdName) {
        return Flux.from("mc-observability/autogen")
                .range(-3L, -1L, ChronoUnit.MINUTES)
                .filter(
                        Restrictions.and(
                                Restrictions.measurement().equal(dto.measurement()),
                                Restrictions.field().equal(dto.field()),
                                Restrictions.tag(targetIdName).equal(dto.targetId()),
                                Restrictions.tag("ns_id").equal(dto.namespaceId())))
                .aggregateWindow(1L, ChronoUnit.MINUTES, dto.aggregation())
                .keep(new String[] {"_time", "_value", "ns_id", "mci_id", "vm_id"})
                .groupBy(new String[] {"ns_id", "mci_id", "vm_id"})
                .toString();
    }
}
