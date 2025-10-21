package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.manager.dto.plugin.PluginDefDTO;
import com.mcmp.o11ymanager.manager.service.AgentPluginDefServiceImpl;
import com.mcmp.o11ymanager.util.ApiDocumentation;
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
@WebMvcTest(SystemController.class)
@MockBean(JpaMetamodelMappingContext.class)
@ActiveProfiles("test")
class SystemControllerTest {

    private static final String TAG = "[Manager] Environment";

    @Autowired private MockMvc mockMvc;
    @MockBean private AgentPluginDefServiceImpl agentPluginDefServiceImpl;

    @Test
    void getPlugins() throws Exception {
        PluginDefDTO plugin =
                PluginDefDTO.builder()
                        .seq(0L)
                        .name("string")
                        .pluginId("string")
                        .pluginType("string")
                        .build();
        List<PluginDefDTO> pluginList = java.util.Collections.singletonList(plugin);
        when(agentPluginDefServiceImpl.getAllPluginDefinitions()).thenReturn(pluginList);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/api/o11y/monitoring/plugins")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        ApiDocumentation.builder()
                                .tag(TAG)
                                .description("Retrieve plugin list")
                                .summary("GetPlugins")
                                .responseSchema("ResBody<List<PluginDefDTO>>")
                                .responseFields(
                                        fieldString("rs_code", "Response code (e.g., 0000)"),
                                        fieldString("rs_msg", "Response message (e.g., Success)"),
                                        fieldArray("data", "Plugin list"),
                                        fieldNumber("data[].seq", "Plugin sequence (e.g., 1)"),
                                        fieldString("data[].name", "Plugin name (e.g., cpu)"),
                                        fieldString(
                                                "data[].pluginId",
                                                "Plugin ID (e.g.,  [[inputs.cpu]])"),
                                        fieldString(
                                                "data[].pluginType", "Plugin type (e.g.,  INPUT)"),
                                        fieldString("error_message", "Error message"))
                                .build());
        verify(agentPluginDefServiceImpl).getAllPluginDefinitions();
    }
}
