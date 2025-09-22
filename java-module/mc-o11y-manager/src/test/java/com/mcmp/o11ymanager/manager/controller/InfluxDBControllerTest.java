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
                                .description("Retrieve all InfluxDB servers")
                                .summary("GetAllInfluxDB")
                                .responseSchema("InfluxDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (example: 0000)"),
                                        fieldString(
                                                "rs_msg", "Response message (example: Success)"),
                                        fieldArray("data", "List of InfluxDB information"),
                                        fieldNumber("data[].id", "ID (example: 1)"),
                                        fieldString(
                                                "data[].url",
                                                "InfluxDB URL (example: localhost:8086)"),
                                        fieldString(
                                                "data[].database", "Database name (example: db-1)"),
                                        fieldString(
                                                        "data[].username",
                                                        "Username (example: mc-user)")
                                                .optional(),
                                        fieldString(
                                                        "data[].retention_policy",
                                                        "Retention policy (example: autogen)")
                                                .optional(),
                                        fieldString(
                                                        "data[].password",
                                                        "Password (example: mypassword")
                                                .optional(),
                                        fieldString("data[].uid", "UID (example: 1)").optional(),
                                        fieldString("error_message", "Error message"))
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
                                .description("Retrieve InfluxDB measurements")
                                .summary("GetMeasurementFields")
                                .responseSchema("FieldDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (example: 0000)"),
                                        fieldString(
                                                "rs_msg", "Response message (example: Success)"),
                                        fieldArray("data", "List of measurement information"),
                                        fieldString(
                                                "data[].measurement",
                                                "Measurement name(example: cpu)"),
                                        fieldArray("data[].fields", "List of field information"),
                                        fieldString("data[].fields[].key", "Field key"),
                                        fieldString("data[].fields[].type", "Field type"),
                                        fieldString("error_message", "Error message"))
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
                                .description("Retrieve InfluxDB tags")
                                .summary("GetMeasurementTags")
                                .responseSchema("TagDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (example: 0000)"),
                                        fieldString(
                                                "rs_msg", "Response message (example: Success)"),
                                        fieldArray("data", "List of tag information"),
                                        fieldString("data[].measurement", "Measurement name"),
                                        fieldArray("data[].tags", "List of tags"),
                                        fieldString("error_message", "Error message"))
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
                                .description("Retrieve InfluxDB metrics")
                                .summary("QueryMetrics")
                                .requestSchema("MetricRequestDTO")
                                .pathParameters(
                                        paramString("nsId", "NS ID"),
                                        paramString("mciId", "MCI ID"))
                                .requestFields(
                                        fieldString("measurement", "Measurement name"),
                                        fieldString("range", "Query range"),
                                        fieldString("group_time", "Grouping unit").optional(),
                                        fieldArray("group_by", "List of group by fields")
                                                .optional(),
                                        fieldNumber("limit", "Result limit count").optional(),
                                        fieldArray("fields", "List of fields to query"),
                                        fieldString(
                                                        "fields[].function",
                                                        "Aggregation function (e.g., mean, max, etc.)")
                                                .optional(),
                                        fieldString("fields[].field", "Field name").optional(),
                                        fieldArray("conditions", "List of condition filters"),
                                        fieldString("conditions[].key", "Condition key").optional(),
                                        fieldString("conditions[].value", "Condition value")
                                                .optional())
                                .responseSchema("MetricDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (example: 0000)"),
                                        fieldString(
                                                "rs_msg", "Response message (example: Success)"),
                                        fieldArray("data", "List of metric information"),
                                        fieldString("data[].name", "Measurement name"),
                                        fieldArray("data[].columns", "List of column names"),
                                        fieldObject("data[].tags", "Tag information").optional(),
                                        fieldSubsection(
                                                "data[].values",
                                                JsonFieldType.ARRAY,
                                                "List of values (2D array, each row corresponds to columns order)"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(influxDbFacadeService).getMetrics(any(), any(), any());
    }
}
