package com.mcmp.o11ymanager.trigger.application.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.trigger.application.service.NotiService;
import com.mcmp.o11ymanager.trigger.application.service.TriggerService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.NotiChannelDetailDto;
import com.mcmp.o11ymanager.util.ApiDocumentation;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc
@WebMvcTest(NotiController.class)
@MockBean(JpaMetamodelMappingContext.class)
@ActiveProfiles("test")
public class NotiControllerTest {

    private static final String TAG = "[Trigger] Notification";

    @Autowired private MockMvc mockMvc;

    @MockBean private TriggerService triggerService;

    @MockBean private NotiService notiService;

    @Test
    void getNotiChannels() throws Exception {
        when(notiService.getNotiChannels())
                .thenReturn(
                        List.of(
                                NotiChannelDetailDto.builder()
                                        .id(1)
                                        .name("sms_naver-cloud")
                                        .type("sms")
                                        .baseUrl("https://sens.apigw.ntruss.com")
                                        .provider("naver-cloud")
                                        .isActive(true)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build()));

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/trigger/noti/channel"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get supported notification channels")
                                .summary("GetSupportedNotificationChannels")
                                .responseSchema("NotiChannelAllResponse")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldObject("data", "Response data"),
                                        fieldArray(
                                                "data.notiChannels", "notification channel list"),
                                        fieldNumber(
                                                "data.notiChannels[].id",
                                                "notification channel id"),
                                        fieldString(
                                                "data.notiChannels[].name",
                                                "notification channel name"),
                                        fieldString(
                                                "data.notiChannels[].type",
                                                "notification channel type"),
                                        fieldString(
                                                "data.notiChannels[].provider",
                                                "notification channel provider"),
                                        fieldString(
                                                "data.notiChannels[].baseUrl",
                                                "notification channel baseUrl"),
                                        fieldBoolean(
                                                "data.notiChannels[].isActive",
                                                "notification channel active status"),
                                        fieldString(
                                                "data.notiChannels[].createdAt",
                                                "notification channel created at"),
                                        fieldString(
                                                "data.notiChannels[].updatedAt",
                                                "notification channel updated at"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"))
                                .build());

        verify(notiService).getNotiChannels();
    }

    @Test
    void getNotiHistories() throws Exception {
        when(notiService.getNotiHistories(any(Pageable.class))).thenReturn(CustomPageDto.empty());

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/api/o11y/trigger/noti/history")
                                .param("page", "1")
                                .param("size", "20")
                                .param("sortBy", "createdAt")
                                .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get paginated notification histories")
                                .summary("GetPaginatedNotificationHistories")
                                .responseSchema("NotiHistoryPageResponse")
                                .queryParameters(
                                        paramInteger("page", "page number (1 .. N)").optional(),
                                        paramInteger("size", "size of page (1 .. N)").optional(),
                                        paramString("sortBy", "sort by properties(id..)")
                                                .optional(),
                                        paramString("sortDirection", "sort direction (asc, desc)")
                                                .optional())
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldObject("data", "Response data"),
                                        fieldArray("data.content", "notification history list"),
                                        fieldSubsection("data.pageable", "specific page info"),
                                        fieldNumber("data.totalPages", "total pages"),
                                        fieldNumber("data.totalElements", "total elements"),
                                        fieldNumber("data.numberOfElements", "number of elements"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"))
                                .build());

        verify(notiService).getNotiHistories(any(Pageable.class));
    }
}
