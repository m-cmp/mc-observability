package com.mcmp.o11ymanager.trigger.application.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldArray;
import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldEnum;
import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldNumber;
import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldObject;
import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldString;
import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldSubsection;
import static com.mcmp.o11ymanager.util.ApiDocumentation.paramInteger;
import static com.mcmp.o11ymanager.util.ApiDocumentation.paramString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerVMDto;
import com.mcmp.o11ymanager.trigger.application.common.type.AggregationType;
import com.mcmp.o11ymanager.trigger.application.common.type.ResourceType;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.*;
import com.mcmp.o11ymanager.trigger.application.service.TriggerService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyCreateDto;
import com.mcmp.o11ymanager.util.ApiDocumentation;
import com.mcmp.o11ymanager.util.JsonConverter;
import java.util.List;
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
@WebMvcTest(TriggerController.class)
@MockBean(JpaMetamodelMappingContext.class)
@ActiveProfiles("test")
class TriggerControllerTest {

    private static final String TAG = "[Trigger] Monitoring Measurement Trigger";

    @Autowired private MockMvc mockMvc;

    @MockBean private TriggerService triggerService;

    @Test
    void createTriggerPolicy() throws Exception {
        TriggerPolicyCreateRequest request =
                TriggerPolicyCreateRequest.builder()
                        .title("CPU Alert Policy")
                        .description("Alert when CPU usage is high")
                        .thresholdCondition(new ThresholdCondition(30, 50, 70))
                        .resourceType(ResourceType.CPU)
                        .aggregationType(AggregationType.LAST)
                        .holdDuration("0m")
                        .repeatInterval("1h")
                        .build();

        when(triggerService.createTriggerPolicy(any(TriggerPolicyCreateDto.class))).thenReturn(1L);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/o11y/trigger/policy")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isCreated()) // Changed to 201 Created
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Create trigger policy")
                                .summary("CreateTriggerPolicy")
                                .requestSchema("TriggerPolicyCreateRequest")
                                .requestFields(
                                        fieldString("title", "trigger policy title"),
                                        fieldString("description", "trigger policy description"),
                                        fieldObject(
                                                "thresholdCondition", "threshold condition object"),
                                        fieldNumber(
                                                "thresholdCondition.info",
                                                "threshold value for info level(1~100)"),
                                        fieldNumber(
                                                "thresholdCondition.warning",
                                                "threshold value for warning level(1~100)"),
                                        fieldNumber(
                                                "thresholdCondition.critical",
                                                "threshold value for critical level(1~100)"),
                                        fieldEnum(
                                                "resourceType",
                                                "resource type",
                                                ResourceType.class),
                                        fieldEnum(
                                                "aggregationType",
                                                "aggregation type",
                                                AggregationType.class),
                                        fieldString(
                                                "holdDuration",
                                                "minimum duration for firing alert(0s~1h)"),
                                        fieldString(
                                                "repeatInterval",
                                                "repeat interval of evaluation(1m~24h)"))
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldObject("data", "Response data"),
                                        fieldNumber("data.id", "Created trigger policy ID"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"))
                                .build());

        verify(triggerService).createTriggerPolicy(any(TriggerPolicyCreateDto.class));
    }

    @Test
    void deleteTriggerPolicy() throws Exception {
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/o11y/trigger/policy/{id}", 1))
                .andExpect(status().isAccepted()) // 202 Accepted
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Delete trigger policy")
                                .summary("DeleteTriggerPolicy")
                                .pathParameters(paramInteger("id", "trigger policy id"))
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldString(
                                                        "data",
                                                        "Response data (null for delete operation)")
                                                .optional(),
                                        fieldString(
                                                        "error_message",
                                                        "Error message (empty if success)")
                                                .optional())
                                .build());

        verify(triggerService).deleteTriggerPolicy(any(long.class));
    }

    @Test
    void getTriggerPolicies() throws Exception {
        when(triggerService.getTriggerPolicies(any(Pageable.class)))
                .thenReturn(CustomPageDto.empty());

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/api/o11y/trigger/policy")
                                .param("page", "1")
                                .param("size", "10")
                                .param("sortBy", "id")
                                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get paginated trigger policies")
                                .summary("GetPaginatedTriggerPolicies")
                                .responseSchema("TriggerPolicyPageResponse")
                                .queryParameters(
                                        paramInteger("page", "page number (1 .. N)").optional(),
                                        paramInteger("size", "size of page (1 .. N)").optional(),
                                        paramString("sortBy", "sort by properties").optional(),
                                        paramString("direction", "sort direction (asc, desc)")
                                                .optional())
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldObject("data", "Response data"),
                                        fieldArray("data.content", "trigger policy list"),
                                        fieldSubsection("data.pageable", "specific page info"),
                                        fieldNumber("data.totalPages", "total pages"),
                                        fieldNumber("data.totalElements", "total elements"),
                                        fieldNumber("data.numberOfElements", "number of elements"),
                                        fieldString(
                                                "error_message",
                                                "Error message (empty if success)"))
                                .build());

        verify(triggerService).getTriggerPolicies(any(Pageable.class));
    }

    @Test
    void addTriggerVM() throws Exception {
        TriggerVMAddRequest request = new TriggerVMAddRequest("first-ns", "mci", "test01");

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/o11y/trigger/policy/{id}/vm", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isAccepted()) // 202 Accepted
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Add trigger VM")
                                .summary("AddTriggerVM")
                                .requestSchema("TriggerVMAddRequest")
                                .pathParameters(paramInteger("id", "trigger policy id"))
                                .requestFields(
                                        fieldString("namespaceId", "Namespace ID"),
                                        fieldString("targetScope", "Target scope (e.g., vm, mci)"),
                                        fieldString("targetId", "Target ID (e.g., vm_id, mci_id)"))
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldString(
                                                        "data",
                                                        "Response data (null for add operation)")
                                                .optional(),
                                        fieldString(
                                                        "error_message",
                                                        "Error message (empty if success)")
                                                .optional())
                                .build());

        verify(triggerService).addTriggerVM(any(long.class), any(TriggerVMDto.class));
    }

    @Test
    void updateTriggerPolicyNotiChannel() throws Exception {
        List<TriggerPolicyNotiChannelUpdateRequest> request =
                List.of(
                        new TriggerPolicyNotiChannelUpdateRequest(
                                "kakao", List.of("+82-10-1234-5678")),
                        new TriggerPolicyNotiChannelUpdateRequest(
                                "sms", List.of("+82-10-1234-5678")),
                        new TriggerPolicyNotiChannelUpdateRequest(
                                "email", List.of("admin@example.com")),
                        new TriggerPolicyNotiChannelUpdateRequest("slack", List.of("C09GRESEF")));

        mockMvc.perform(
                        RestDocumentationRequestBuilders.put(
                                        "/api/o11y/trigger/policy/{id}/channel", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isAccepted()) // 202 Accepted
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Update trigger policy notification channels")
                                .summary("UpdateTriggerPolicyNotificationChannels")
                                .requestSchema("TriggerPolicyNotiChannelUpdateRequest")
                                .pathParameters(paramInteger("id", "trigger policy id"))
                                .requestFields(
                                        fieldArray(
                                                "[]",
                                                "List of notification channel update objects"),
                                        fieldString(
                                                "[].channelName",
                                                "Notification channel name (e.g., kakao, sms, email, slack)"),
                                        fieldArray("[].recipients", "List of recipients"))
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldString(
                                                        "data",
                                                        "Response data (null for update operation)")
                                                .optional(),
                                        fieldString(
                                                        "error_message",
                                                        "Error message (empty if success)")
                                                .optional())
                                .build());

        verify(triggerService)
                .updateTriggerPolicyNotiChannelByName(any(long.class), any(List.class));
    }

    @Test
    void removeTriggerVM() throws Exception {
        TriggerVMRemoveRequest request = new TriggerVMRemoveRequest("namespace-1", "vm", "vm-1");

        mockMvc.perform(
                        RestDocumentationRequestBuilders.delete(
                                        "/api/o11y/trigger/policy/{id}/vm", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isAccepted()) // 202 Accepted
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Remove trigger VM")
                                .summary("RemoveTriggerVM")
                                .requestSchema("TriggerVMRemoveRequest")
                                .pathParameters(paramInteger("id", "trigger policy id"))
                                .requestFields(
                                        fieldString("namespaceId", "Namespace ID"),
                                        fieldString("targetScope", "Target scope (e.g., vm, mci)"),
                                        fieldString("targetId", "Target ID (e.g., vm_id, mci_id)"))
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., success)"),
                                        fieldString(
                                                        "data",
                                                        "Response data (null for remove operation)")
                                                .optional(),
                                        fieldString(
                                                        "error_message",
                                                        "Error message (empty if success)")
                                                .optional())
                                .build());

        verify(triggerService).removeTriggerVM(any(long.class), any(TriggerVMDto.class));
    }
}
