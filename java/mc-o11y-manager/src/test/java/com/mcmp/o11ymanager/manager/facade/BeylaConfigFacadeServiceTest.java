package com.mcmp.o11ymanager.manager.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.mcmp.o11ymanager.manager.service.interfaces.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BeylaConfigFacadeServiceTest {

    @Mock private FileService fileService;

    @InjectMocks private BeylaConfigFacadeService service;

    private static final String SITE_CODE = "test-site";
    private static final String OTEL_ENDPOINT = "http://test-tempo:4317";
    private static final String NS_ID = "ns-1";
    private static final String MCI_ID = "mci-1";
    private static final String VM_ID = "vm-1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "deploySiteCode", SITE_CODE);
        ReflectionTestUtils.setField(service, "otelEndpoint", OTEL_ENDPOINT);

        // FileService.appendConfig(ClassPathResource, StringBuilder) 가 호출되면
        // 실제 ClassPath의 beyla_template.yaml을 읽어 sb에 append하도록 mock 동작 정의
        doAnswer(
                        invocation -> {
                            ClassPathResource resource = invocation.getArgument(0);
                            StringBuilder sb = invocation.getArgument(1);
                            try (var is = resource.getInputStream()) {
                                sb.append(new String(is.readAllBytes()));
                            }
                            return null;
                        })
                .when(fileService)
                .appendConfig(any(ClassPathResource.class), any(StringBuilder.class));
    }

    @Test
    @DisplayName("initBeylaConfig: 모든 placeholder가 치환됨")
    void initBeylaConfig_replacesAllPlaceholders() {
        String result = service.initBeylaConfig(NS_ID, MCI_ID, VM_ID);

        assertThat(result).doesNotContain("@SITE_CODE");
        assertThat(result).doesNotContain("@OTEL_ENDPOINT");
        assertThat(result).doesNotContain("@NS_ID");
        assertThat(result).doesNotContain("@MCI_ID");
        assertThat(result).doesNotContain("@VM_ID");
    }

    @Test
    @DisplayName("initBeylaConfig: SITE_CODE가 deploy.site-code 값으로 치환됨")
    void initBeylaConfig_substitutesSiteCode() {
        String result = service.initBeylaConfig(NS_ID, MCI_ID, VM_ID);

        assertThat(result).contains("cmp-beyla-" + SITE_CODE);
    }

    @Test
    @DisplayName("initBeylaConfig: OTEL_ENDPOINT가 application.yaml 값으로 치환됨")
    void initBeylaConfig_substitutesOtelEndpoint() {
        String result = service.initBeylaConfig(NS_ID, MCI_ID, VM_ID);

        assertThat(result).contains(OTEL_ENDPOINT);
        // metrics와 traces 두 곳에 모두 적용되는지
        assertThat(result.split(OTEL_ENDPOINT, -1).length - 1)
                .as("OTEL_ENDPOINT는 metrics와 traces 두 곳에 등장해야 함")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("initBeylaConfig: nsId/mciId/vmId가 null이면 빈 문자열로 치환")
    void initBeylaConfig_nullIds_handledAsEmpty() {
        String result = service.initBeylaConfig(null, null, null);

        assertThat(result).doesNotContain("@NS_ID");
        assertThat(result).doesNotContain("@MCI_ID");
        assertThat(result).doesNotContain("@VM_ID");
    }
}
