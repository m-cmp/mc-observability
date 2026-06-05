package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.gpu.GpuMetricFieldCheckDTO;
import com.mcmp.o11ymanager.manager.dto.gpu.GpuMetricKeyDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.facade.GpuMonitoringFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GPU(DCGM Exporter) 메트릭 조회 API (o11y-manager GpuMonitoringController 포팅).
 *
 * <p>대상 노드에 DCGM Exporter가 설치되어 있고 telegraf가 GPU 수집 설정(prometheus input + starlark)을 포함해야 데이터가
 * 존재한다. (VM 등록 시 gpu=true 또는 Ansible enable_gpu=true)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/gpu")
@Tag(name = "[Manager] GPU Monitoring Metric")
public class GpuMonitoringController {

    private final GpuMonitoringFacadeService gpuMonitoringFacadeService;

    @GetMapping("/list")
    @Operation(
            summary = "GetGpuMetricFields",
            operationId = "GetGpuMetricFields",
            description =
                    "Retrieve GPU(DCGM) metric categories and field definitions (name, description, unit)")
    public ResBody<List<GpuMetricKeyDTO>> getGpuMetricFields() {
        return new ResBody<>(gpuMonitoringFacadeService.getGpuMetricKeys());
    }

    @GetMapping("/field/check/{nsId}/{mciId}/{vmId}")
    @Operation(
            summary = "CheckGpuMetricFields",
            operationId = "CheckGpuMetricFields",
            description =
                    "Check whether each GPU metric field has data within the recent 5 minutes")
    public ResBody<List<GpuMetricFieldCheckDTO>> checkGpuMetricFields(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId) {
        return new ResBody<>(gpuMonitoringFacadeService.checkGpuMetricFields(nsId, mciId, vmId));
    }

    @PostMapping("/metric/{nsId}/{mciId}/{vmId}")
    @Operation(
            summary = "GetGpuMetrics",
            operationId = "GetGpuMetrics",
            description =
                    "Retrieve GPU(DCGM) metrics. The measurement is fixed to `dcgm`; requested fields are validated against the GPU metric field definitions. Use group_by/conditions with DCGM tags (e.g., gpu, UUID, modelName) to query per-GPU series.")
    public ResBody<List<MetricDTO>> getGpuMetrics(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId,
            @RequestBody MetricRequestDTO req) {
        return new ResBody<>(gpuMonitoringFacadeService.getGpuMetrics(nsId, mciId, vmId, req));
    }
}
