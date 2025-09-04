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
    private static final String TAG = "[System] environment";

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
                                .description("플러그인 목록 조회")
                                .summary("GetPlugins")
                                .responseSchema("ResBody<List<PluginDefDTO>>")
                                .responseFields(
                                        fieldString("rs_code", "응답 코드"),
                                        fieldString("rs_msg", "응답 메시지"),
                                        fieldArray("data", "플러그인 목록"),
                                        fieldNumber("data[].seq", "플러그인 시퀀스"),
                                        fieldString("data[].name", "플러그인 이름"),
                                        fieldString("data[].pluginId", "플러그인 ID"),
                                        fieldString("data[].pluginType", "플러그인 타입"),
                                        fieldString("error_message", "에러 메시지"))
                                .build());
        verify(agentPluginDefServiceImpl).getAllPluginDefinitions();
    }
}
