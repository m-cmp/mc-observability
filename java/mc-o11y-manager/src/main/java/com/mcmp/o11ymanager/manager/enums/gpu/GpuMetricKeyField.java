package com.mcmp.o11ymanager.manager.enums.gpu;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * GPU 메트릭 정의.
 *
 * <p>telegraf의 nvidia_smi input이 `nvidia-smi -q -x`를 읽고 starlark processor가 그 필드를 InfluxDB `dcgm`
 * measurement의 필드명으로 변환한다. 여기서 "DCGM"은 수집 데몬이 아니라 <b>스키마 이름</b>이다 - 수집기는 바뀌었지만 필드명은 그대로라
 * 조회·대시보드·알람은 영향을 받지 않는다.
 *
 * <p>여기 정의된 필드는 전부 {@code telegraf_processors_nvidia_smi_to_dcgm}이 실제로 만들어낼 수 있는 것이다. 패치된 telegraf
 * 플러그인이나 커널 로그가 있어야 하는 항목(PCIe replay, vGPU 라이선스, XID, NVLink, 누적 에너지)은 수집 경로가 없어 정의에서 제외했다.
 */
public class GpuMetricKeyField {

    /** GPU 메트릭의 measurement 이름 (InfluxDB) */
    public static final String GPU_MEASUREMENT = "dcgm";

    @Getter
    @AllArgsConstructor
    public enum GpuMetricKey {

        // 매트릭 구분용
        CLOCKS("clocks", GpuMetricField.CLOCKS),
        TEMPERATURE("temperature", GpuMetricField.TEMPERATURE),
        POWER("power", GpuMetricField.POWER),
        UTILIZATION("utilization", GpuMetricField.UTILIZATION),
        ERRORS("errors", GpuMetricField.ERRORS),
        MEMORY_USAGE("memory_usage", GpuMetricField.MEMORY_USAGE),
        ECC("ecc", GpuMetricField.ECC),
        REMAPPED_ROWS("remapped_rows", GpuMetricField.REMAPPED_ROWS),

        // 통합 관리용
        DCGM("dcgm", GpuMetricField.ALL),
        ;

        private final String key;
        private final List<GpuMetricField> fields;

        private static final Map<String, GpuMetricKey> FIELD_TO_KEY_MAP;

        static {
            Map<String, GpuMetricKey> map = new HashMap<>();
            for (GpuMetricKey key : GpuMetricKey.values()) {
                for (GpuMetricField metricField : key.fields) {
                    map.put(metricField.getFieldName().toLowerCase(), key);
                }
            }
            FIELD_TO_KEY_MAP = Collections.unmodifiableMap(map);
        }

        public static GpuMetricKey fromField(String field) {
            GpuMetricKey key = FIELD_TO_KEY_MAP.get(field.toLowerCase());
            if (key != null) return key;
            throw new IllegalArgumentException("Unknown GPU metric field: " + field);
        }

        /** 메트릭 조회 시 노출할 카테고리 목록 (통합 관리용 DCGM 제외) */
        public static List<GpuMetricKey> getMonitoringKeys() {
            return Arrays.asList(
                    CLOCKS,
                    TEMPERATURE,
                    POWER,
                    UTILIZATION,
                    ERRORS,
                    MEMORY_USAGE,
                    ECC,
                    REMAPPED_ROWS);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum GpuMetricField {

        // Clocks
        SM_CLOCK("SM 클럭 주파수", "sm_clock", "MHz"),
        MEM_CLOCK("메모리 클럭 주파수", "mem_clock", "MHz"),

        // Temperature
        MEMORY_TEMP("메모리 온도", "memory_temp", "C"),
        GPU_TEMP("GPU 온도", "gpu_temp", "C"),
        FAN_SPEED("쿨러 팬 속도", "fan_speed", "%"),

        // Power
        POWER_USAGE("전력 사용량", "power_usage", "W"),
        P_STATE("GPU 성능 상태 단계", "p_state", "level"),

        // Utilization
        GPU_UTIL("GPU 사용률", "gpu_util", "%"),
        MEM_COPY_UTIL("메모리 사용률", "mem_copy_util", "%"),
        ENC_UTIL("인코더 사용률", "enc_util", "%"),
        DEC_UTIL("디코더 사용률", "dec_util", "%"),

        // Errors And Violations
        CLOCKS_EVENT_REASONS("클럭 스로틀링 발생 사유", "clocks_event_reasons", "bitmask"),

        // Memory Usage
        FB_TOTAL("전체 프레임 버퍼 메모리", "fb_total", "MB"),
        FB_FREE("프레임 버퍼 사용 가능 메모리", "fb_free", "MB"),
        FB_USED("사용 중인 프레임 버퍼 메모리", "fb_used", "MB"),

        // ECC (Datacenter GPU only, A100, H100, V100, T4)
        ECC_SBE_VOL_TOTAL("단일 비트 휘발성 ECC 오류 누적 횟수", "ecc_sbe_vol_total", "count"),
        ECC_DBE_VOL_TOTAL("이중 비트 휘발성 ECC 오류 누적 횟수", "ecc_dbe_vol_total", "count"),
        ECC_SBE_AGG_TOTAL("단일 비트 영구 ECC 오류 누적 횟수", "ecc_sbe_agg_total", "count"),
        ECC_DBE_AGG_TOTAL("이중 비트 영구 ECC 오류 누적 횟수", "ecc_dbe_agg_total", "count"),

        // Remapped rows
        UNCORRECTABLE_REMAPPED_ROWS(
                "복구 불가능한 오류로 인해 리매핑된 메모리 행 수", "uncorrectable_remapped_rows", "count"),
        CORRECTABLE_REMAPPED_ROWS(
                "복구 가능한 오류로 인해 리매핑된 메모리 행 수", "correctable_remapped_rows", "count"),
        ROW_REMAP_FAILURE("메모리 행 리매핑 실패 여부", "row_remap_failure", "boolean"),
        ;

        private final String fieldDesc; // 필드 설명
        private final String fieldName; // InfluxDB에서 사용하는 필드명
        private final String unit; // 필드의 단위

        private static final Map<String, GpuMetricField> FIELD_NAME_MAP;

        static {
            Map<String, GpuMetricField> map = new HashMap<>();
            for (GpuMetricField field : GpuMetricField.values()) {
                map.put(field.getFieldName().toLowerCase(), field);
            }
            FIELD_NAME_MAP = Collections.unmodifiableMap(map);
        }

        public static GpuMetricField fromMetricName(String target) {
            GpuMetricField field = FIELD_NAME_MAP.get(target.toLowerCase());
            if (field != null) return field;
            throw new IllegalArgumentException("Invalid GPU metric field: " + target);
        }

        private static final List<GpuMetricField> CLOCKS = Arrays.asList(SM_CLOCK, MEM_CLOCK);

        private static final List<GpuMetricField> TEMPERATURE =
                Arrays.asList(MEMORY_TEMP, GPU_TEMP, FAN_SPEED);

        private static final List<GpuMetricField> POWER = Arrays.asList(POWER_USAGE, P_STATE);

        private static final List<GpuMetricField> UTILIZATION =
                Arrays.asList(GPU_UTIL, MEM_COPY_UTIL, ENC_UTIL, DEC_UTIL);

        private static final List<GpuMetricField> ERRORS = Arrays.asList(CLOCKS_EVENT_REASONS);

        private static final List<GpuMetricField> MEMORY_USAGE =
                Arrays.asList(FB_TOTAL, FB_FREE, FB_USED);

        private static final List<GpuMetricField> ECC =
                Arrays.asList(
                        ECC_SBE_VOL_TOTAL, ECC_DBE_VOL_TOTAL, ECC_SBE_AGG_TOTAL, ECC_DBE_AGG_TOTAL);

        private static final List<GpuMetricField> REMAPPED_ROWS =
                Arrays.asList(
                        UNCORRECTABLE_REMAPPED_ROWS, CORRECTABLE_REMAPPED_ROWS, ROW_REMAP_FAILURE);

        public static final List<GpuMetricField> TOTAL_FIELDS = Arrays.asList(FB_FREE, FB_USED);

        public static final List<GpuMetricField> ALL =
                Stream.of(
                                CLOCKS,
                                TEMPERATURE,
                                POWER,
                                UTILIZATION,
                                ERRORS,
                                MEMORY_USAGE,
                                ECC,
                                REMAPPED_ROWS)
                        .flatMap(Collection::stream)
                        .toList();

        /** fb_free/fb_used처럼 전체 대비 사용량을 갖는 필드의 '전체' 필드를 반환 */
        public GpuMetricField getTotalMetric() {
            switch (this) {
                case FB_FREE:
                case FB_USED:
                    return GpuMetricField.FB_TOTAL;

                default:
                    return this;
            }
        }
    }
}
