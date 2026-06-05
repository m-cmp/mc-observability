package com.mcmp.o11ymanager.manager.dto.gpu;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/** 특정 GPU 메트릭 필드에 최근 데이터가 쌓이고 있는지 확인용 응답 */
@Builder
public record GpuMetricFieldCheckDTO(
        @Schema(description = "InfluxDB field name", example = "gpu_util") @JsonProperty("field")
                String field,
        @Schema(description = "Whether data exists within the recent window", example = "true")
                @JsonProperty("has_data")
                boolean hasData) {}
