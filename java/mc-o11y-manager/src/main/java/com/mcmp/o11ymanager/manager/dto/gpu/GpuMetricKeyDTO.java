package com.mcmp.o11ymanager.manager.dto.gpu;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

/** GPU 메트릭 카테고리(key)와 소속 필드 정의 목록 */
@Builder
public record GpuMetricKeyDTO(
        @Schema(description = "GPU metric category key", example = "utilization")
                @JsonProperty("key")
                String key,
        @Schema(description = "InfluxDB measurement name", example = "dcgm")
                @JsonProperty("measurement")
                String measurement,
        @Schema(description = "GPU metric fields of this category") @JsonProperty("fields")
                List<GpuMetricFieldDTO> fields) {

    /** 단일 GPU 메트릭 필드 정의 */
    @Builder
    public record GpuMetricFieldDTO(
            @Schema(description = "InfluxDB field name", example = "gpu_util") @JsonProperty("name")
                    String name,
            @Schema(description = "Field description", example = "GPU 사용률")
                    @JsonProperty("description")
                    String description,
            @Schema(description = "Field unit", example = "%") @JsonProperty("unit") String unit) {}
}
