package mcmp.mc.observability.o11ymanager.application.controller;

import static mcmp.mc.observability.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import com.mcmp.o11ymanager.trigger.application.controller.TriggerHistoryController;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerHistoryCommentUpdateRequest;
import com.mcmp.o11ymanager.trigger.application.service.TriggerHistoryService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerHistoryCommentUpdateDto;
import mcmp.mc.observability.o11ymanager.util.ApiDocumentation;
import mcmp.mc.observability.o11ymanager.util.JsonConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc
@WebMvcTest(TriggerHistoryController.class)
class TriggerHistoryControllerTest {

    private static final String TAG = "TRIGGER-HISTORY";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TriggerHistoryService triggerHistoryService;

    @Test
    void getTriggerHistories() throws Exception {
        when(triggerHistoryService.getTriggerHistories(any(Pageable.class)))
                .thenReturn(CustomPageDto.empty());

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/o11y/trigger/history")
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
                                        fieldArray("content", "trigger history list"),
                                        fieldSubsection("pageable", "specific page info"),
                                        fieldNumber("totalPages", "total pages"),
                                        fieldNumber("totalElements", "total elements"),
                                        fieldNumber("numberOfElements", "number of elements"))
                                .build());

        verify(triggerHistoryService).getTriggerHistories(any(Pageable.class));
    }

    @Test
    void updateTriggerHistoryComment() throws Exception {
        TriggerHistoryCommentUpdateRequest request =
                new TriggerHistoryCommentUpdateRequest("Alert resolved - false positive");

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/o11y/trigger/history/{id}/comment", 1L)
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
