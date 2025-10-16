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

    private static final String TAG = "[Manager] Monitoring Metric";

    @Autowired private MockMvc mockMvc;
    @MockBean private InfluxDbFacadeService influxDbFacadeService;

    @Test
    void getAllInfluxDB() throws Exception {
        List<InfluxDTO> influxList =
                List.of(
                        InfluxDTO.builder()
                                .id(0L)
                                .url("mcmp:8086")
                                .database("mydb")
                                .username("mc-user")
                                .retention_policy("autogen")
                                .password("mypw")
                                .uid("sdfsj1df33ff")
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
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldArray("data", "List of InfluxDB information"),
                                        fieldNumber("data[].id", "ID (e.g.,  1)"),
                                        fieldString(
                                                "data[].url",
                                                "InfluxDB URL (e.g., localhost:8086)"),
                                        fieldString(
                                                "data[].database", "Database name (e.g.,  db1)"),
                                        fieldString("data[].username", "Username (e.g., mc-user)")
                                                .optional(),
                                        fieldString(
                                                        "data[].retention_policy",
                                                        "Retention policy (e.g., autogen)")
                                                .optional(),
                                        fieldString("data[].password", "Password (e.g., mypw")
                                                .optional(),
                                        fieldString("data[].uid", "UID (e.g.,  1)").optional(),
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
                                                        .key("cpu")
                                                        .type("Integer")
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
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldArray("data", "List of measurement information"),
                                        fieldString(
                                                "data[].measurement",
                                                "Measurement name(e.g., cpu)"),
                                        fieldArray("data[].fields", "List of field information"),
                                        fieldString(
                                                "data[].fields[].key",
                                                "Field key (e.g., server_time)"),
                                        fieldString(
                                                "data[].fields[].type",
                                                "Field type (e.g., integer)"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(influxDbFacadeService).getFields();
    }

    @Test
    void tag() throws Exception {
        List<TagDTO> tagList =
                List.of(
                        TagDTO.builder()
                                .measurement("cpu")
                                .tags(List.of("host", "mci_id"))
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
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldArray("data", "List of tag information"),
                                        fieldString(
                                                "data[].measurement",
                                                "Measurement name (e.g., cpu)"),
                                        fieldArray("data[].tags", "List of tags"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(influxDbFacadeService).getTags();
    }

    @Test
    void getMetricsByNsIdAndMciId() throws Exception {
        String nsId = "ns-1";
        String mciId = "mci-1";
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement("cpu");
        req.setRange("1h");
        req.setGroupTime("12m");
        req.setGroupBy(List.of("vm_id"));
        req.setLimit(0L);
        MetricRequestDTO.FieldInfo fieldInfo = new MetricRequestDTO.FieldInfo();
        fieldInfo.setFunction("mean");
        fieldInfo.setField("usage_idle");
        req.setFields(List.of(fieldInfo));
        MetricRequestDTO.ConditionInfo condInfo = new MetricRequestDTO.ConditionInfo();
        condInfo.setKey("cpu");
        condInfo.setValue("cpu-total");
        req.setConditions(List.of(condInfo));
        List<MetricDTO> metricList =
                List.of(
                        MetricDTO.builder()
                                .name("cpu")
                                .columns(List.of("cpu", "ns_id"))
                                .values(List.of(List.of("string", 0)))
                                .build());
        when(influxDbFacadeService.postMetricsByNsMci(any(), any(), any())).thenReturn(metricList);

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
                                .summary("GetMetricsByNsIdAndMciId")
                                .requestSchema("MetricRequestDTO")
                                .pathParameters(
                                        paramString("nsId", "nsId (e.g., ns-1)"),
                                        paramString("mciId", "mciId (e.g., mci-1)"))
                                .requestFields(
                                        fieldString("measurement", "Measurement name (e.g., cpu)"),
                                        fieldString("range", "Query range (e.g., 1h)"),
                                        fieldString("group_time", "Grouping unit").optional(),
                                        fieldArray(
                                                        "group_by",
                                                        "List of group-by fields (e.g., vm_id)")
                                                .optional(),
                                        fieldNumber("limit", "Result limit count").optional(),
                                        fieldArray("fields", "List of fields to query"),
                                        fieldString(
                                                        "fields[].function",
                                                        "Aggregation function (e.g., mean, max, etc.)")
                                                .optional(),
                                        fieldString(
                                                        "fields[].field",
                                                        "Field name (e.g., usage_idle)")
                                                .optional(),
                                        fieldArray("conditions", "List of condition filters"),
                                        fieldString("conditions[].key", "Condition key (e.g., cpu)")
                                                .optional(),
                                        fieldString(
                                                        "conditions[].value",
                                                        "Condition value (e.g., cpu-total)")
                                                .optional())
                                .responseSchema("MetricDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g.,  Success)"),
                                        fieldArray("data", "List of metric information"),
                                        fieldString("data[].name", "Measurement name (e.g., cpu)"),
                                        fieldArray("data[].columns", "List of column names"),
                                        fieldObject("data[].tags", "Tag information").optional(),
                                        fieldSubsection(
                                                "data[].values",
                                                JsonFieldType.ARRAY,
                                                "List of values (2D array, each row corresponds to columns order)"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(influxDbFacadeService).postMetricsByNsMci(any(), any(), any());
    }

    @Test
    void getMetricsByVMId() throws Exception {
        String nsId = "ns-1";
        String mciId = "mci-1";
        String vmId = "vm-1";
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement("cpu");
        req.setRange("1h");
        req.setGroupTime("12m");
        req.setGroupBy(List.of("vm_id"));
        req.setLimit(0L);
        MetricRequestDTO.FieldInfo fieldInfo = new MetricRequestDTO.FieldInfo();
        fieldInfo.setFunction("mean");
        fieldInfo.setField("usage_idle");
        req.setFields(List.of(fieldInfo));
        MetricRequestDTO.ConditionInfo condInfo = new MetricRequestDTO.ConditionInfo();
        condInfo.setKey("cpu");
        condInfo.setValue("cpu-total");
        req.setConditions(List.of(condInfo));
        List<MetricDTO> metricList =
                List.of(
                        MetricDTO.builder()
                                .name("cpu")
                                .columns(List.of("cpu", "vm_id"))
                                .values(List.of(List.of("string", 0)))
                                .build());
        when(influxDbFacadeService.postMetricsByVM(any(), any(), any(), any()))
                .thenReturn(metricList);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/monitoring/influxdb/metric/{nsId}/{mciId}/{vmId}",
                                        nsId,
                                        mciId,
                                        vmId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(req)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Retrieve InfluxDB metrics")
                                .summary("GetMetricsByVMId")
                                .requestSchema("MetricRequestDTO")
                                .pathParameters(
                                        paramString("nsId", "nsId (e.g., ns-1)"),
                                        paramString("mciId", "mciId (e.g., mci-1)"),
                                        paramString("vmId", "vmId (e.g., vm-1)"))
                                .requestFields(
                                        fieldString("measurement", "Measurement name (e.g., cpu)"),
                                        fieldString("range", "Query range (e.g., 1h)"),
                                        fieldString("group_time", "Grouping unit").optional(),
                                        fieldArray(
                                                        "group_by",
                                                        "List of group-by fields (e.g., vm_id)")
                                                .optional(),
                                        fieldNumber("limit", "Result limit count").optional(),
                                        fieldArray("fields", "List of fields to query"),
                                        fieldString(
                                                        "fields[].function",
                                                        "Aggregation function (e.g., mean, max, etc.)")
                                                .optional(),
                                        fieldString(
                                                        "fields[].field",
                                                        "Field name (e.g., usage_idle)")
                                                .optional(),
                                        fieldArray("conditions", "List of condition filters"),
                                        fieldString("conditions[].key", "Condition key (e.g., cpu)")
                                                .optional(),
                                        fieldString(
                                                        "conditions[].value",
                                                        "Condition value (e.g., cpu-total)")
                                                .optional())
                                .responseSchema("MetricDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g.,  Success)"),
                                        fieldArray("data", "List of metric information"),
                                        fieldString("data[].name", "Measurement name (e.g., cpu)"),
                                        fieldArray("data[].columns", "List of column names"),
                                        fieldObject("data[].tags", "Tag information").optional(),
                                        fieldSubsection(
                                                "data[].values",
                                                JsonFieldType.ARRAY,
                                                "List of values (2D array, each row corresponds to columns order)"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(influxDbFacadeService).postMetricsByVM(any(), any(), any(), any());
    }
}
