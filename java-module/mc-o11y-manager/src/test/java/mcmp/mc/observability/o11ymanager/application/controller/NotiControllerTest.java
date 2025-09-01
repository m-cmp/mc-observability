package mcmp.mc.observability.o11ymanager.application.controller;

import static mcmp.mc.observability.o11ymanager.util.ApiDocumentation.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mcmp.o11ymanager.trigger.application.controller.NotiController;
import com.mcmp.o11ymanager.trigger.application.service.NotiService;
import com.mcmp.o11ymanager.trigger.application.service.TriggerService;
import com.mcmp.o11ymanager.trigger.application.service.dto.NotiChannelDetailDto;
import java.time.LocalDateTime;
import java.util.List;
import mcmp.mc.observability.o11ymanager.util.ApiDocumentation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc
@WebMvcTest(NotiController.class)
public class NotiControllerTest {

  private static final String TAG = "NOTIFICATION";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TriggerService triggerService;

  @MockitoBean
  private NotiService notiService;

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

    mockMvc.perform(MockMvcRequestBuilders.get("/api/o11y/noti/channel"))
        .andExpect(status().isOk())
        .andDo(
            ApiDocumentation.builder()
                .tag(TAG)
                .description("Get supported notification channels")
                .summary("GetSupportedNotificationChannels")
                .responseSchema("NotiChannelAllResponse")
                .responseFields(
                    fieldArray("notiChannels", "notification channel list"),
                    fieldNumber("notiChannels[].id", "notification channel id"),
                    fieldString(
                        "notiChannels[].name", "notification channel name"),
                    fieldString(
                        "notiChannels[].type", "notification channel type"),
                    fieldString(
                        "notiChannels[].provider",
                        "notification channel provider"),
                    fieldString(
                        "notiChannels[].baseUrl",
                        "notification channel baseUrl"),
                    fieldBoolean(
                        "notiChannels[].isActive",
                        "notification channel active status"),
                    fieldString(
                        "notiChannels[].createdAt",
                        "notification channel created at"),
                    fieldString(
                        "notiChannels[].updatedAt",
                        "notification channel updated at"))
                .build());

    verify(notiService).getNotiChannels();
  }

  @Test
  void getNotiHistories() throws Exception {
    when(notiService.getNotiHistories(any(Pageable.class))).thenReturn(CustomPageDto.empty());

    mockMvc.perform(
            MockMvcRequestBuilders.get("/api/o11y/noti/history")
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
                    paramString("sortBy", "sort by properties").optional(),
                    paramString("sortDirection", "sort direction (asc, desc)")
                        .optional())
                .responseFields(
                    fieldArray("content", "notification history list"),
                    fieldSubsection("pageable", "specific page info"),
                    fieldNumber("totalPages", "total pages"),
                    fieldNumber("totalElements", "total elements"),
                    fieldNumber("numberOfElements", "number of elements"))
                .build());

    verify(notiService).getNotiHistories(any(Pageable.class));
  }
}
