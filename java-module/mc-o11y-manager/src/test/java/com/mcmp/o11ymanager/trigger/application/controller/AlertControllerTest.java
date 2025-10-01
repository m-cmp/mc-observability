package com.mcmp.o11ymanager.trigger.application.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldArray;
import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldBoolean;
import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldNumber;
import static com.mcmp.o11ymanager.util.ApiDocumentation.fieldSubsection;
import static com.mcmp.o11ymanager.util.ApiDocumentation.paramInteger;
import static com.mcmp.o11ymanager.util.ApiDocumentation.paramString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.trigger.adapter.external.alert.AlertManager;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.GrafanaHealthCheckResponse;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.GrafanaHealthCheckResponse.HealthStatus;
import com.mcmp.o11ymanager.trigger.application.service.AlertService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.util.ApiDocumentation;
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
@WebMvcTest(AlertController.class)
@ActiveProfiles("test")
@MockBean(JpaMetamodelMappingContext.class)
class AlertControllerTest {

    private static final String TAG = "[Trigger - Only Developer] Monitoring Trigger Event Handler";

    @Autowired private MockMvc mockMvc;

    @MockBean private AlertManager alertManager;
    @MockBean private AlertService alertService;

    @Test
    void checkGrafanaHealth() throws Exception {
        GrafanaHealthCheckResponse healthResponse =
                GrafanaHealthCheckResponse.builder()
                        .contactPoint(new HealthStatus(true))
                        .datasource(new HealthStatus(true))
                        .folder(new HealthStatus(true))
                        .org(new HealthStatus(true))
                        .build();

        when(alertManager.checkGrafanaHealth()).thenReturn(healthResponse);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/trigger/alert/health"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Check grafana health status")
                                .summary("CheckGrafanaHealthStatus")
                                .responseSchema("GrafanaHealthCheckResponse")
                                .responseFields(
                                        fieldBoolean(
                                                "contactPoint.hasData",
                                                "contact point is set up correctly"),
                                        fieldBoolean(
                                                "datasource.hasData",
                                                "datasource is set up correctly"),
                                        fieldBoolean(
                                                "folder.hasData", "folder is set up correctly"),
                                        fieldBoolean("org.hasData", "org is set up correctly"))
                                .build());

        verify(alertManager).checkGrafanaHealth();
    }

    @Test
    void getAllAlerts() throws Exception {
        when(alertManager.getAllAlerts()).thenReturn(List.of());

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/trigger/alert/alerts"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get all alerts")
                                .summary("GetAllAlerts")
                                .responseSchema("AllAlertsResponse")
                                .responseFields(fieldArray("[]", "List of alerts"))
                                .build());

        verify(alertManager).getAllAlerts();
    }

    @Test
    void getAlertByTitle() throws Exception {
        when(alertManager.getAlertBy(any(String.class))).thenReturn(List.of());

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                        "/api/o11y/trigger/alert/alerts/search")
                                .param("title", "CPU Alert"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Search alerts by title")
                                .summary("SearchAlertsByTitle")
                                .responseSchema("AlertResponse")
                                .queryParameters(paramString("title", "alert title to search"))
                                .responseFields(fieldArray("[]", "List of matching alerts"))
                                .build());

        verify(alertManager).getAlertBy("CPU Alert");
    }

    @Test
    void getAllAlertRules() throws Exception {
        when(alertManager.getAllAlertRules()).thenReturn(List.of());

        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/o11y/trigger/alert/alert-rules"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get all alert rules")
                                .summary("GetAllAlertRules")
                                .responseSchema("AllAlertRulesResponse")
                                .responseFields(fieldArray("[]", "List of alert rules"))
                                .build());

        verify(alertManager).getAllAlertRules();
    }

    @Test
    void getAllContactPoints() throws Exception {
        when(alertManager.getAllContactPoints()).thenReturn(List.of());

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get(
                                "/api/o11y/trigger/alert/contact-points"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get all contact points")
                                .summary("GetAllContactPoints")
                                .responseSchema("GrafanaManagedReceiverConfig")
                                .responseFields(fieldArray("[]", "List of contact points"))
                                .build());

        verify(alertManager).getAllContactPoints();
    }

    @Test
    void testAlertReceiver() throws Exception {
        mockMvc.perform(
                        RestDocumentationRequestBuilders.post(
                                "/api/o11y/trigger/alert/alert-receiver/test"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Test alert receiver connection")
                                .summary("TestAlertReceiverConnection")
                                .build());

        verify(alertManager).testAlertReceiver();
    }

    @Test
    void getAlertTestHistories() throws Exception {
        when(alertService.getAlertTestHistories(any(Pageable.class)))
                .thenReturn(CustomPageDto.empty());

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/api/o11y/trigger/alert/test-history")
                                .param("page", "1")
                                .param("size", "10")
                                .param("sortBy", "id")
                                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Get paginated alert test histories")
                                .summary("GetPaginatedAlertTestHistories")
                                .responseSchema("AlertTestHistoryPageResponse")
                                .queryParameters(
                                        paramInteger("page", "page number (1 .. N)").optional(),
                                        paramInteger("size", "size of page (1 .. N)").optional(),
                                        paramString("sortBy", "sort by properties (id..)")
                                                .optional(),
                                        paramString("direction", "sort direction (asc, desc)")
                                                .optional())
                                .responseFields(
                                        fieldArray("content", "alert test history list"),
                                        fieldSubsection("pageable", "specific page info"),
                                        fieldNumber("totalPages", "total pages"),
                                        fieldNumber("totalElements", "total elements"),
                                        fieldNumber("numberOfElements", "number of elements"))
                                .build());

        verify(alertService).getAlertTestHistories(any(Pageable.class));
    }
}
