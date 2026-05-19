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
class OtelJavaConfigFacadeServiceTest {

    @Mock private FileService fileService;

    @InjectMocks private OtelJavaConfigFacadeService service;

    private static final String SITE_CODE = "test-site";
    private static final String OTEL_ENDPOINT = "http://test-collector:14317";
    private static final String JAR_URL =
            "https://example.com/opentelemetry-javaagent.jar";
    private static final String JAR_PATH = "C:\\opentelemetry\\opentelemetry-javaagent.jar";
    private static final String NS_ID = "ns-1";
    private static final String MCI_ID = "mci-1";
    private static final String VM_ID = "vm-1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "deploySiteCode", SITE_CODE);
        ReflectionTestUtils.setField(service, "otelEndpoint", OTEL_ENDPOINT);
        ReflectionTestUtils.setField(service, "jarUrl", JAR_URL);
        ReflectionTestUtils.setField(service, "jarPath", JAR_PATH);

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
    @DisplayName("initOtelJavaConfig: 모든 placeholder가 치환됨")
    void initOtelJavaConfig_replacesAllPlaceholders() {
        String result = service.initOtelJavaConfig(NS_ID, MCI_ID, VM_ID);

        assertThat(result).doesNotContain("@SITE_CODE");
        assertThat(result).doesNotContain("@OTEL_ENDPOINT");
        assertThat(result).doesNotContain("@NS_ID");
        assertThat(result).doesNotContain("@MCI_ID");
        assertThat(result).doesNotContain("@VM_ID");
        assertThat(result).doesNotContain("@JAR_URL");
        assertThat(result).doesNotContain("@JAR_PATH");
    }

    @Test
    @DisplayName("initOtelJavaConfig: OTEL_ENDPOINT/JAR_URL/JAR_PATH가 application.yaml 값으로 치환됨")
    void initOtelJavaConfig_substitutesValues() {
        String result = service.initOtelJavaConfig(NS_ID, MCI_ID, VM_ID);

        assertThat(result).contains(OTEL_ENDPOINT);
        assertThat(result).contains(JAR_URL);
        assertThat(result).contains(JAR_PATH);
    }

    @Test
    @DisplayName("initOtelJavaConfig: SITE_CODE/VM_ID가 OTEL_SERVICE_NAME에 합성됨")
    void initOtelJavaConfig_serviceNameComposition() {
        String result = service.initOtelJavaConfig(NS_ID, MCI_ID, VM_ID);

        assertThat(result).contains("cmp-otel-java-" + SITE_CODE + "-" + VM_ID);
    }

    @Test
    @DisplayName("initOtelJavaConfig: JAVA_TOOL_OPTIONS에 -javaagent + jar-path가 포함됨")
    void initOtelJavaConfig_javaToolOptions() {
        String result = service.initOtelJavaConfig(NS_ID, MCI_ID, VM_ID);

        assertThat(result).contains("JAVA_TOOL_OPTIONS=-javaagent:" + JAR_PATH);
    }

    @Test
    @DisplayName("initOtelJavaConfig: nsId/mciId/vmId가 null이면 빈 문자열로 치환")
    void initOtelJavaConfig_nullIds_handledAsEmpty() {
        String result = service.initOtelJavaConfig(null, null, null);

        assertThat(result).doesNotContain("@NS_ID");
        assertThat(result).doesNotContain("@MCI_ID");
        assertThat(result).doesNotContain("@VM_ID");
    }
}
