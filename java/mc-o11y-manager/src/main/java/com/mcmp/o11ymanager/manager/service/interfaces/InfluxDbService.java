package com.mcmp.o11ymanager.manager.service.interfaces;

import com.mcmp.o11ymanager.manager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.manager.dto.influx.VmRef;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import java.util.List;

public interface InfluxDbService {

    InfluxDTO get(Long id);

    boolean isConnectedDb(InfluxDTO influxDTO);

    Long resolveInfluxDb(String nsId, String infraId);

    ResBody<List<FieldDTO>> getFields();

    ResBody<List<TagDTO>> getTags();

    String fetchDefaultRp(InfluxDTO influxDTO);

    List<MetricDTO> getMetricsByNsMci(String nsId, String infraId, MetricRequestDTO req);

    List<MetricDTO> getMetricsByVM(
            String nsId, String infraId, String nodeId, MetricRequestDTO req);

    List<InfluxDTO> rawServers();

    InfluxDTO resolveInfluxDto(String nsId, String infraId);

    /**
     * Returns distinct (ns_id, infra_id, node_id) tuples that currently have metric data in any
     * configured InfluxDB instance. Used by the cache warmer to discover VMs to pre-load.
     */
    List<VmRef> discoverActiveVms();

    /**
     * Loads metrics for a VM and stores them in the cache, bypassing the fresh window.
     *
     * <p>Warming must not go through {@link #getMetricsByVM} — that path returns whatever is
     * already cached, so repeated warming ticks would never refresh anything.
     */
    List<MetricDTO> refreshMetricsByVM(
            String nsId, String infraId, String nodeId, MetricRequestDTO req);

    /** True when the cache already holds a fresh entry for this query, so warming can skip it. */
    boolean isMetricCacheFresh(String nsId, String infraId, String nodeId, MetricRequestDTO req);

    /**
     * Measurement names this specific VM reports, memoised. Warming every known measurement against
     * every VM produced mostly-empty queries; this narrows it to what actually exists.
     */
    List<String> measurementsOfVm(String nsId, String infraId, String nodeId);
}
