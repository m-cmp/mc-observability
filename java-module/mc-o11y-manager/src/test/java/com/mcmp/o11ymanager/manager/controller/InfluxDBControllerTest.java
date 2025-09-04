package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.manager.dto.influx.FieldDTO;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.dto.influx.TagDTO;
import com.mcmp.o11ymanager.manager.facade.InfluxDbFacadeService;
import com.mcmp.o11ymanager.util.ApiDocumentation;
import com.mcmp.o11ymanager.util.JsonConverter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc
@WebMvcTest(InfluxDBController.class)
@MockBean(JpaMetamodelMappingContext.class)
@ActiveProfiles("test")
class InfluxDBControllerTest {

    private static final String TAG = "[Monitoring metric] Monitoring metric";

    @Autowired private MockMvc mockMvc;
    @MockBean private InfluxDbFacadeService influxDbFacadeService;

    @Test
    void getAllInfluxDB() throws Exception {
        List<InfluxDTO> influxList =
                List.of(
                        InfluxDTO.builder()
                                .id(0L)
                                .url("string")
                                .database("string")
                                .username("string")
                                .retention_policy("string")
                                .password("string")
                                .uid("string")
                                .build());
        when(influxDbFacadeService.getInfluxDbs()).thenReturn(influxList);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/monitoring/influxdb"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("InfluxDB 서버 전체 조회")
                                .summary("GetAllInfluxDB")
                                .responseSchema("InfluxDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "InfluxDB 정보 목록"),
                                        fieldNumber("data[].id", "ID"),
                                        fieldString("data[].url", "InfluxDB URL"),
                                        fieldString("data[].database", "DB명"),
                                        fieldString("data[].username", "사용자명").optional(),
                                        fieldString("data[].retention_policy", "보존 정책").optional(),
                                        fieldString("data[].password", "비밀번호").optional(),
                                        fieldString("data[].uid", "UID").optional(),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(influxDbFacadeService).getInfluxDbs();
    }

    @Test
    void measurement() throws Exception {
        List<FieldDTO> fieldList =
                List.of(
                        FieldDTO.builder()
                                .measurement("string")
                                .fields(
                                        List.of(
                                                FieldDTO.FieldInfo.builder()
                                                        .key("string")
                                                        .type("string")
                                                        .build()))
                                .build());
        when(influxDbFacadeService.getFields()).thenReturn(fieldList);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/monitoring/influxdb/measurement"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("InfluxDB 측정항목 조회")
                                .summary("GetMeasurementFields")
                                .responseSchema("FieldDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "측정항목 정보 목록"),
                                        fieldString("data[].measurement", "측정항목명"),
                                        fieldArray("data[].fields", "필드 정보 목록"),
                                        fieldString("data[].fields[].key", "필드 키"),
                                        fieldString("data[].fields[].type", "필드 타입"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(influxDbFacadeService).getFields();
    }

    @Test
    void tag() throws Exception {
        List<TagDTO> tagList =
                List.of(
                        TagDTO.builder()
                                .measurement("string")
                                .tags(List.of("string", "string"))
                                .build());
        when(influxDbFacadeService.getTags()).thenReturn(tagList);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/monitoring/influxdb/tag"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("InfluxDB 태그 조회")
                                .summary("GetMeasurementTags")
                                .responseSchema("TagDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "태그 정보 목록"),
                                        fieldString("data[].measurement", "측정항목명"),
                                        fieldArray("data[].tags", "태그 목록"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(influxDbFacadeService).getTags();
    }

    @Test
    void query() throws Exception {
        String nsId = "string";
        String mciId = "string";
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement("string");
        req.setRange("string");
        req.setGroupTime("string");
        req.setGroupBy(List.of("string"));
        req.setLimit(0L);
        MetricRequestDTO.FieldInfo fieldInfo = new MetricRequestDTO.FieldInfo();
        fieldInfo.setFunction("string");
        fieldInfo.setField("string");
        req.setFields(List.of(fieldInfo));
        MetricRequestDTO.ConditionInfo condInfo = new MetricRequestDTO.ConditionInfo();
        condInfo.setKey("string");
        condInfo.setValue("string");
        req.setConditions(List.of(condInfo));
        List<MetricDTO> metricList =
                List.of(
                        MetricDTO.builder()
                                .name("string")
                                .columns(List.of("string", "string"))
                                .values(List.of(List.of("string", 0)))
                                .build());
        when(influxDbFacadeService.getMetrics(any(), any(), any())).thenReturn(metricList);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/monitoring/influxdb/metric/{nsId}/{mciId}",
                                        nsId,
                                        mciId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(req)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("InfluxDB 메트릭 조회")
                                .summary("QueryMetrics")
                                .requestSchema("MetricRequestDTO")
                                .pathParameters(
                                        paramString("nsId", "NS ID"),
                                        paramString("mciId", "MCI ID"))
                                .requestFields(
                                        fieldString("measurement", "측정항목명"),
                                        fieldString("range", "조회 범위"),
                                        fieldString("group_time", "그룹핑 단위").optional(),
                                        fieldArray("group_by", "Group by 필드 목록").optional(),
                                        fieldNumber("limit", "결과 제한 개수").optional(),
                                        fieldArray("fields", "조회할 필드 목록"),
                                        fieldString("fields[].function", "집계 함수 (예: mean, max 등)")
                                                .optional(),
                                        fieldString("fields[].field", "필드 이름").optional(),
                                        fieldArray("conditions", "조건 필터 목록"),
                                        fieldString("conditions[].key", "조건 키").optional(),
                                        fieldString("conditions[].value", "조건 값").optional())
                                .responseSchema("MetricDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "메트릭 정보 목록"),
                                        fieldString("data[].name", "측정항목명"),
                                        fieldArray("data[].columns", "컬럼명 목록"),
                                        fieldObject("data[].tags", "태그 정보").optional(),
                                        fieldSubsection(
                                                "data[].values",
                                                JsonFieldType.ARRAY,
                                                "값 목록 (2차원 배열, 각 행은 columns 순서에 맞는 값들)"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(influxDbFacadeService).getMetrics(any(), any(), any());
    }
}
