package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemRequestDTO;
import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemUpdateDTO;
import com.mcmp.o11ymanager.manager.facade.ItemFacadeService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc
@WebMvcTest(ItemController.class)
@ActiveProfiles("test")
@MockBean(JpaMetamodelMappingContext.class)
class ItemControllerTest {

    private static final String TAG = "[Monitoring item] Monitoring vm item management";

    @Autowired private MockMvc mockMvc;
    @MockBean private ItemFacadeService itemFacadeService;

    @Test
    void getItems() throws Exception {
        List<MonitoringItemDTO> items =
                List.of(
                        MonitoringItemDTO.builder()
                                .seq(0L)
                                .nsId("string")
                                .mciId("string")
                                .vmId("string")
                                .name("string")
                                .state("string")
                                .pluginSeq(0L)
                                .pluginName("string")
                                .pluginType("string")
                                .pluginConfig("string")
                                .build());
        when(itemFacadeService.getTelegrafItems(any(), any(), any())).thenReturn(items);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}/item",
                                "ns1",
                                "mci1",
                                "vm1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Retrieve monitoring item list")
                                .summary("GetMonitoringItems")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("vmId", "TARGET ID"))
                                .responseSchema("MonitoringItemDTO")
                                .responseFields(
                                        fieldString("rs_code", "Response code (example: 0000)"),
                                        fieldString(
                                                "rs_msg", "Response message (example: Success)"),
                                        fieldArray("data", "Monitoring item list"),
                                        fieldNumber("data[].seq", "Item sequence"),
                                        fieldString("data[].nsId", "NSID"),
                                        fieldString("data[].mciId", "MCI ID"),
                                        fieldString("data[].vmId", "TARGET ID"),
                                        fieldString("data[].name", "Item name"),
                                        fieldString("data[].state", "State"),
                                        fieldNumber("data[].pluginSeq", "Plugin sequence"),
                                        fieldString("data[].pluginName", "Plugin name"),
                                        fieldString("data[].pluginType", "Plugin type"),
                                        fieldString("data[].pluginConfig", "Plugin configuration"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(itemFacadeService).getTelegrafItems(any(), any(), any());
    }

    @Test
    void postItem() throws Exception {
        MonitoringItemRequestDTO dto =
                MonitoringItemRequestDTO.builder().pluginSeq(0L).pluginConfig("string").build();

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}/item",
                                        "ns1",
                                        "mci1",
                                        "vm1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(dto)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Add monitoring item")
                                .summary("AddMonitoringItem")
                                .pathParameters(
                                        paramString("nsId", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("vmId", "TARGET ID"))
                                .requestSchema("MonitoringItemRequestDTO")
                                .requestFields(
                                        fieldNumber("pluginSeq", "Plugin sequence"),
                                        fieldString("pluginConfig", "Plugin configuration"))
                                .responseSchema("ResBody<Void>")
                                .responseFields(
                                        fieldString("rs_code", "Response code (example: 0000)"),
                                        fieldString(
                                                "rs_msg", "Response message (example: Success)"),
                                        fieldSubsection("data", "Response data").optional(),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(itemFacadeService)
                .addTelegrafPlugin(any(), any(), any(), any(MonitoringItemRequestDTO.class));
    }

    @Test
    void putItem() throws Exception {
        MonitoringItemUpdateDTO dto =
                MonitoringItemUpdateDTO.builder().seq(0L).pluginConfig("string").build();

        mockMvc.perform(
                        RestDocumentationRequestBuilders.put(
                                        "/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}/item",
                                        "ns1",
                                        "mci1",
                                        "vm1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(dto)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Update monitoring item")
                                .summary("UpdateMonitoringItem")
                                .pathParameters(
                                        paramString("nsId", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("vmId", "TARGET ID"))
                                .requestSchema("MonitoringItemUpdateDTO")
                                .requestFields(
                                        fieldNumber("seq", "Item sequence"),
                                        fieldString("pluginConfig", "Plugin configuration"))
                                .responseSchema("ResBody<Void>")
                                .responseFields(
                                        fieldString("rs_code", "Response code (example: 0000)"),
                                        fieldString(
                                                "rs_msg", "Response message (example: Success)"),
                                        fieldSubsection("data", "Response data").optional(),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(itemFacadeService)
                .updateTelegrafPlugin(any(), any(), any(), any(MonitoringItemUpdateDTO.class));
    }

    @Test
    void deleteItem() throws Exception {
        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete(
                                "/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}/item/{itemSeq}",
                                "ns1",
                                "mci1",
                                "vm1",
                                1L))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Delete monitoring item")
                                .summary("DeleteMonitoringItem")
                                .pathParameters(
                                        paramString("nsId", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("vmId", "TARGET ID"),
                                        paramString("itemSeq", "ITEM SEQ"))
                                .responseSchema("ResBody<Void>")
                                .responseFields(
                                        fieldString("rs_code", "Response code (example: 0000)"),
                                        fieldString(
                                                "rs_msg", "Response message (example: Success)"),
                                        fieldSubsection("data", "Response data").optional(),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(itemFacadeService).deleteTelegrafPlugin(any(), any(), any(), any(Long.class));
    }
}
