package mcmp.mc.observability.o11ymanager.application.controller;

import static mcmp.mc.observability.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerTargetDto;
import com.mcmp.o11ymanager.trigger.application.common.type.AggregationType;
import com.mcmp.o11ymanager.trigger.application.common.type.ResourceType;
import com.mcmp.o11ymanager.trigger.application.controller.TriggerController;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerPolicyCreateRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerPolicyNotiChannelUpdateRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerTargetAddRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerTargetRemoveRequest;
import com.mcmp.o11ymanager.trigger.application.service.TriggerService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyCreateDto;
import java.util.List;
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
@WebMvcTest(TriggerController.class)
class TriggerControllerTest {

    private static final String TAG = "TRIGGER-POLICY";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TriggerService triggerService;

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
                        .repeatInterval("1m")
                        .build();

        when(triggerService.createTriggerPolicy(any(TriggerPolicyCreateDto.class))).thenReturn(1L);

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/o11y/trigger/policy")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isCreated())
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
                                                "threshold value for info level"),
                                        fieldNumber(
                                                "thresholdCondition.warning",
                                                "threshold value for warning level"),
                                        fieldNumber(
                                                "thresholdCondition.critical",
                                                "threshold value for critical level"),
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
                                                "minimum duration for firing alert"),
                                        fieldString(
                                                "repeatInterval", "repeat interval of evaluation"))
                                .build());

        verify(triggerService).createTriggerPolicy(any(TriggerPolicyCreateDto.class));
    }

    @Test
    void deleteTriggerPolicy() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/o11y/trigger/policy/{id}", 1))
                .andExpect(status().isAccepted())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Delete trigger policy")
                                .summary("DeleteTriggerPolicy")
                                .pathParameters(paramInteger("id", "trigger policy id"))
                                .build());

        verify(triggerService).deleteTriggerPolicy(any(long.class));
    }

    @Test
    void getTriggerPolicies() throws Exception {
        when(triggerService.getTriggerPolicies(any(Pageable.class)))
                .thenReturn(CustomPageDto.empty());

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/o11y/trigger/policy")
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
                                        fieldArray("content", "trigger policy list"),
                                        fieldSubsection("pageable", "specific page info"),
                                        fieldNumber("totalPages", "total pages"),
                                        fieldNumber("totalElements", "total elements"),
                                        fieldNumber("numberOfElements", "number of elements"))
                                .build());

        verify(triggerService).getTriggerPolicies(any(Pageable.class));
    }

    @Test
    void updateTriggerPolicyNotiChannel() throws Exception {
        List<TriggerPolicyNotiChannelUpdateRequest> request =
                List.of(
                        new TriggerPolicyNotiChannelUpdateRequest(
                                "sms_ncp", List.of("+82-10-1234-5678", "+82-10-9876-5432")),
                        new TriggerPolicyNotiChannelUpdateRequest(
                                "email_smtp", List.of("admin@example.com", "dev@example.com")));

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/o11y/trigger/policy/{id}/channel", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isAccepted())
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
                                        fieldString("[].channelName", "notification channel name"),
                                        fieldArray("[].recipients", "list of recipients"))
                                .build());

        verify(triggerService)
                .updateTriggerPolicyNotiChannelByName(any(long.class), any(List.class));
    }

    @Test
    void addTriggerTarget() throws Exception {
        TriggerTargetAddRequest request =
                new TriggerTargetAddRequest("namespace-1", "vm", "target-1", true);

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/o11y/trigger/policy/{id}/target", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isAccepted())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Add trigger target")
                                .summary("AddTriggerTarget")
                                .requestSchema("TriggerTargetAddRequest")
                                .pathParameters(paramInteger("id", "trigger policy id"))
                                .requestFields(
                                        fieldString("namespaceId", "namespace id"),
                                        fieldString("targetScope", "target scope (vm, mci)"),
                                        fieldString("targetId", "target id"),
                                        fieldBoolean("isActive", "active status"))
                                .build());

        verify(triggerService).addTriggerTarget(any(long.class), any(TriggerTargetDto.class));
    }

    @Test
    void removeTriggerTarget() throws Exception {
        TriggerTargetRemoveRequest request =
                new TriggerTargetRemoveRequest("namespace-1", "vm", "target-1", true);

        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/api/o11y/trigger/policy/{id}/target", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonConverter.asJsonString(request)))
                .andExpect(status().isAccepted())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Remove trigger target")
                                .summary("RemoveTriggerTarget")
                                .requestSchema("TriggerTargetRemoveRequest")
                                .pathParameters(paramInteger("id", "trigger policy id"))
                                .requestFields(
                                        fieldString("namespaceId", "namespace id"),
                                        fieldString("targetScope", "target scope (vm, mci)"),
                                        fieldString("targetId", "target id"),
                                        fieldBoolean("isActive", "active status"))
                                .build());

        verify(triggerService).removeTriggerTarget(any(long.class), any(TriggerTargetDto.class));
    }
}
