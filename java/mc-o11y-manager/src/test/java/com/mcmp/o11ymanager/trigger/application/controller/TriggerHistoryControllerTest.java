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
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerHistoryDetailDto;
import com.mcmp.o11ymanager.util.ApiDocumentation;
import com.mcmp.o11ymanager.util.JsonConverter;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        TriggerHistoryDetailDto mockHistory =
                new TriggerHistoryDetailDto(
                        13L,
                        "string",
                        "last",
                        "0s",
                        "1h",
                        "cpu",
                        "test01",
                        "test01",
                        "vm-1",
                        "30.0",
                        "51.6",
                        "CRITICAL",
                        "firing",
                        null,
                        LocalDateTime.parse("2025-10-17T15:32:10"),
                        LocalDateTime.parse("2025-10-17T06:32:10.112181"),
                        LocalDateTime.parse("2025-10-17T06:32:10.112186"));

        CustomPageDto<TriggerHistoryDetailDto> mockPage =
                new CustomPageDto<>(
                        List.of(mockHistory),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                        1,
                        13,
                        13);

        when(triggerHistoryService.getTriggerHistories(any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/api/o11y/trigger/history")
                                .param("page", "1")
                                .param("size", "20")
                                .param("sortBy", "createdAt")
                                .param("sortDirection", "desc")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get paginated trigger histories")
                                .summary("GetPaginatedTriggerHistories")
                                .responseSchema("TriggerHistoryPageResponse")
                                .queryParameters(
                                        paramInteger("page", "Page number (1 .. N)").optional(),
                                        paramInteger("size", "Page size (1 .. N)").optional(),
                                        paramString(
                                                        "sortBy",
                                                        "Property to sort by (e.g., createdAt)")
                                                .optional(),
                                        paramString("sortDirection", "Sort direction (asc, desc)")
                                                .optional())
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldObject("data", "Response data"),
                                        fieldArray("data.content", "List of trigger history items"),
                                        fieldNumber(
                                                "data.content[].id",
                                                "Trigger history ID (e.g., 13)"),
                                        fieldString(
                                                "data.content[].triggerTitle",
                                                "Trigger title (e.g., string)"),
                                        fieldString(
                                                "data.content[].aggregationType",
                                                "Aggregation type (e.g., last)"),
                                        fieldString(
                                                "data.content[].holdDuration",
                                                "Hold duration before alert (e.g., 0s)"),
                                        fieldString(
                                                "data.content[].repeatInterval",
                                                "Repeat interval (e.g., 1h)"),
                                        fieldString(
                                                "data.content[].resourceType",
                                                "Resource type (e.g., cpu, memory)"),
                                        fieldString(
                                                "data.content[].namespaceId",
                                                "Namespace ID (e.g., test01)"),
                                        fieldString(
                                                "data.content[].mciId", "MCI ID (e.g., test01)"),
                                        fieldString("data.content[].vmId", "VM ID (e.g., vm-1)"),
                                        fieldString(
                                                "data.content[].threshold",
                                                "Alert threshold value (e.g., 30.0)"),
                                        fieldString(
                                                "data.content[].resourceUsage",
                                                "Actual resource usage (e.g., 51.6)"),
                                        fieldString(
                                                "data.content[].alertLevel",
                                                "Alert level (e.g., INFO, WARNING, CRITICAL)"),
                                        fieldString(
                                                "data.content[].status",
                                                "Alert status (e.g., firing, resolved)"),
                                        fieldString(
                                                        "data.content[].comment",
                                                        "Additional comment (nullable)")
                                                .optional(),
                                        fieldString(
                                                "data.content[].startsAt",
                                                "Alert start time (e.g., 2025-10-17T15:32:10)"),
                                        fieldString(
                                                "data.content[].createdAt",
                                                "Creation timestamp (e.g., 2025-10-17T06:32:10.112181)"),
                                        fieldString(
                                                "data.content[].updatedAt",
                                                "Last updated timestamp (e.g., 2025-10-17T06:32:10.112186)"),
                                        fieldSubsection(
                                                "data.pageable",
                                                "Pagination metadata (page number, size, sort info)"),
                                        fieldNumber(
                                                "data.totalPages",
                                                "Total number of pages (e.g., 1)"),
                                        fieldNumber(
                                                "data.totalElements",
                                                "Total number of elements (e.g., 13)"),
                                        fieldNumber(
                                                "data.numberOfElements",
                                                "Number of elements in this page (e.g., 13)"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success, e.g., '')"))
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
