package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.gpu.GpuMetricFieldCheckDTO;
import com.mcmp.o11ymanager.manager.dto.gpu.GpuMetricKeyDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.enums.gpu.GpuMetricKeyField;
import com.mcmp.o11ymanager.manager.enums.gpu.GpuMetricKeyField.GpuMetricField;
import com.mcmp.o11ymanager.manager.enums.gpu.GpuMetricKeyField.GpuMetricKey;
import com.mcmp.o11ymanager.manager.service.interfaces.InfluxDbService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * GPU(DCGM) 메트릭 조회 facade (o11y-manager GpuMonitoringFacadeService 포팅).
 *
 * <p>GPU 메트릭은 telegraf가 DCGM Exporter를 스크랩해 InfluxDB의 `dcgm` measurement로 저장하며, 조회는 기존
 * InfluxDbService 경로(ns/infra/node 태그 필터)를 그대로 사용한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GpuMonitoringFacadeService {

    /** 필드 데이터 존재 확인 시 조회 범위 */
    private static final String CHECK_RANGE = "5m";

    private final InfluxDbService influxDbService;

    /** GPU 메트릭 카테고리/필드 정의 목록 조회 */
    public List<GpuMetricKeyDTO> getGpuMetricKeys() {
        return GpuMetricKey.getMonitoringKeys().stream()
                .map(
                        key ->
                                GpuMetricKeyDTO.builder()
                                        .key(key.getKey())
                                        .measurement(GpuMetricKeyField.GPU_MEASUREMENT)
                                        .fields(
                                                key.getFields().stream()
                                                        .map(
                                                                field ->
                                                                        GpuMetricKeyDTO
                                                                                .GpuMetricFieldDTO
                                                                                .builder()
                                                                                .name(
                                                                                        field
                                                                                                .getFieldName())
                                                                                .description(
                                                                                        field
                                                                                                .getFieldDesc())
                                                                                .unit(
                                                                                        field
                                                                                                .getUnit())
                                                                                .build())
                                                        .toList())
                                        .build())
                .toList();
    }

    /** 각 GPU 메트릭 필드에 최근(5분) 데이터가 쌓이고 있는지 확인 */
    public List<GpuMetricFieldCheckDTO> checkGpuMetricFields(
            String nsId, String mciId, String vmId) {

        List<GpuMetricFieldCheckDTO> results = new ArrayList<>();

        for (GpuMetricField field : GpuMetricField.ALL) {
            MetricRequestDTO req = new MetricRequestDTO();
            req.setMeasurement(GpuMetricKeyField.GPU_MEASUREMENT);
            req.setRange(CHECK_RANGE);
            req.setLimit(1L);

            MetricRequestDTO.FieldInfo fieldInfo = new MetricRequestDTO.FieldInfo();
            fieldInfo.setFunction("last");
            fieldInfo.setField(field.getFieldName());
            req.setFields(List.of(fieldInfo));

            boolean hasData;
            try {
                List<MetricDTO> metrics = influxDbService.getMetricsByVM(nsId, mciId, vmId, req);
                hasData =
                        metrics != null
                                && metrics.stream()
                                        .anyMatch(
                                                m ->
                                                        m.values() != null
                                                                && !m.values().isEmpty()
                                                                && m.values().stream()
                                                                        .anyMatch(
                                                                                row ->
                                                                                        row.size()
                                                                                                        > 1
                                                                                                && row
                                                                                                                .get(
                                                                                                                        1)
                                                                                                        != null));
            } catch (Exception e) {
                log.warn(
                        "Failed to check GPU metric field {} for {}/{}/{}: {}",
                        field.getFieldName(),
                        nsId,
                        mciId,
                        vmId,
                        e.getMessage());
                hasData = false;
            }

            results.add(
                    GpuMetricFieldCheckDTO.builder()
                            .field(field.getFieldName())
                            .hasData(hasData)
                            .build());
        }

        return results;
    }

    /** GPU 메트릭 조회: 요청 필드를 검증하고 measurement를 dcgm으로 고정해 조회 */
    public List<MetricDTO> getGpuMetrics(
            String nsId, String mciId, String vmId, MetricRequestDTO req) {

        // 요청 필드가 정의된 GPU 메트릭 필드인지 검증
        if (req.getFields() != null) {
            for (MetricRequestDTO.FieldInfo fieldInfo : req.getFields()) {
                GpuMetricField.fromMetricName(fieldInfo.getField());
            }
        }

        // GPU 메트릭은 항상 dcgm measurement에서 조회
        req.setMeasurement(GpuMetricKeyField.GPU_MEASUREMENT);

        return influxDbService.getMetricsByVM(nsId, mciId, vmId, req);
    }
}
