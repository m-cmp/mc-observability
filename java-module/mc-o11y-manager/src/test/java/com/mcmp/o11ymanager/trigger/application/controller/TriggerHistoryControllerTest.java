package com.mcmp.o11ymanager.trigger.application.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerHistoryCommentUpdateRequest;
import com.mcmp.o11ymanager.trigger.application.service.TriggerHistoryService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerHistoryCommentUpdateDto;
import com.mcmp.o11ymanager.util.ApiDocumentation;
import com.mcmp.o11ymanager.util.JsonConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc
@WebMvcTest(TriggerHistoryController.class)
@MockBean(JpaMetamodelMappingContext.class)
@ActiveProfiles("test")
class TriggerHistoryControllerTest {

    private static final String TAG = "[Trigger] Monitoring Measurement Trigger";

    @Autowired private MockMvc mockMvc;

    @MockBean private TriggerHistoryService triggerHistoryService;

    @Test
    void getTriggerHistories() throws Exception {
        when(triggerHistoryService.getTriggerHistories(any(Pageable.class)))
                .thenReturn(CustomPageDto.empty());

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/api/o11y/trigger/history")
                                .param("page", "1")
                                .param("size", "20")
                                .param("sortBy", "createdAt")
                                .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get paginated trigger histories")
                                .summary("GetPaginatedTriggerHistories")
                                .responseSchema("TriggerHistoryPageResponse")
                                .queryParameters(
                                        paramInteger("page", "page number (1 .. N)").optional(),
                                        paramInteger("size", "size of page (1 .. N)").optional(),
                                        paramString("sortBy", "sort by properties").optional(),
                                        paramString("sortDirection", "sort direction (asc, desc)")
                                                .optional())
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldObject("data", "Response data"),
                                        fieldArray("data.content", "trigger history list"),
                                        fieldSubsection("data.pageable", "specific page info"),
                                        fieldNumber("data.totalPages", "total pages"),
                                        fieldNumber("data.totalElements", "total elements"),
                                        fieldNumber("data.numberOfElements", "number of elements"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"))
                                .build());

        verify(triggerHistoryService).getTriggerHistories(any(Pageable.class));
    }

    @Test
    void updateTriggerHistoryComment() throws Exception {
        TriggerHistoryCommentUpdateRequest request =
                new TriggerHistoryCommentUpdateRequest("Alert resolved - false positive");

        mockMvc.perform(
                        RestDocumentationRequestBuilders.put(
                                        "/api/o11y/trigger/history/{id}/comment", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Update trigger history comment")
                                .summary("UpdateTriggerHistoryComment")
                                .requestSchema("TriggerHistoryCommentUpdateRequest")
                                .pathParameters(paramInteger("id", "trigger history id"))
                                .requestFields(fieldString("comment", "comment to update"))
                                .build());

        verify(triggerHistoryService)
                .updateComment(any(long.class), any(TriggerHistoryCommentUpdateDto.class));
    }
}
