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
    private static final String TAG = "[Monitoring item] Monitoring target item management";

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
                                .targetId("string")
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
                                "/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}/item",
                                "ns1",
                                "mci1",
                                "target1"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("모니터링 아이템 목록 조회")
                                .summary("GetMonitoringItems")
                                .pathParameters(
                                        paramString("nsId", "NSID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("targetId", "TARGET ID"))
                                .responseSchema("MonitoringItemDTO")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "모니터링 아이템 목록"),
                                        fieldNumber("data[].seq", "아이템 시퀀스"),
                                        fieldString("data[].nsId", "NSID"),
                                        fieldString("data[].mciId", "MCI ID"),
                                        fieldString("data[].targetId", "TARGET ID"),
                                        fieldString("data[].name", "아이템 이름"),
                                        fieldString("data[].state", "상태"),
                                        fieldNumber("data[].pluginSeq", "플러그인 시퀀스"),
                                        fieldString("data[].pluginName", "플러그인 이름"),
                                        fieldString("data[].pluginType", "플러그인 타입"),
                                        fieldString("data[].pluginConfig", "플러그인 설정"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(itemFacadeService).getTelegrafItems(any(), any(), any());
    }

    @Test
    void postItem() throws Exception {
        MonitoringItemRequestDTO dto =
                MonitoringItemRequestDTO.builder().pluginSeq(0L).pluginConfig("string").build();

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                        "/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}/item",
                                        "ns1",
                                        "mci1",
                                        "target1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(dto)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("모니터링 아이템 추가")
                                .summary("AddMonitoringItem")
                                .pathParameters(
                                        paramString("nsId", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("targetId", "TARGET ID"))
                                .requestSchema("MonitoringItemRequestDTO")
                                .requestFields(
                                        fieldNumber("pluginSeq", "플러그인 시퀀스"),
                                        fieldString("pluginConfig", "플러그인 설정"))
                                .responseSchema("ResBody<Void>")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldSubsection("data", "응답 데이터").optional(),
                                        fieldString("error_message", "에러 메시지"))
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
                                        "/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}/item",
                                        "ns1",
                                        "mci1",
                                        "target1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(dto)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("모니터링 아이템 수정")
                                .summary("UpdateMonitoringItem")
                                .pathParameters(
                                        paramString("nsId", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("targetId", "TARGET ID"))
                                .requestSchema("MonitoringItemUpdateDTO")
                                .requestFields(
                                        fieldNumber("seq", "아이템 시퀀스"),
                                        fieldString("pluginConfig", "플러그인 설정"))
                                .responseSchema("ResBody<Void>")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldSubsection("data", "응답 데이터").optional(),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(itemFacadeService)
                .updateTelegrafPlugin(any(), any(), any(), any(MonitoringItemUpdateDTO.class));
    }

    @Test
    void deleteItem() throws Exception {
        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete(
                                "/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}/item/{itemSeq}",
                                "ns1",
                                "mci1",
                                "target1",
                                1L))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("모니터링 아이템 삭제")
                                .summary("DeleteMonitoringItem")
                                .pathParameters(
                                        paramString("nsId", "NS ID"),
                                        paramString("mciId", "MCI ID"),
                                        paramString("targetId", "TARGET ID"),
                                        paramString("itemSeq", "ITEM SEQ"))
                                .responseSchema("ResBody<Void>")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldSubsection("data", "응답 데이터").optional(),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(itemFacadeService).deleteTelegrafPlugin(any(), any(), any(), any(Long.class));
    }
}
