package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.service.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Windows OTel Java Auto-Instrumentation 환경변수 묶음 생성 담당.
 *
 * <p>{@link BeylaConfigFacadeService} 패턴을 그대로 본떠, ClassPath의
 * {@code otel_java_template.properties}를 읽어 placeholder를 치환한 결과를 반환한다. Ansible playbook은 이 결과를
 * Windows 호스트에 배포하고 KEY=VALUE 줄을 시스템 환경변수로 설정한다.
 *
 * <p>치환 placeholder: {@code @SITE_CODE}, {@code @NS_ID}, {@code @MCI_ID}, {@code @VM_ID},
 * {@code @OTEL_ENDPOINT}, {@code @JAR_URL}, {@code @JAR_PATH}.
 *
 * <p>OTEL endpoint는 Beyla와 공용으로 application.yaml의 {@code beyla.otel-endpoint}를 재사용한다 (POC 후
 * {@code tracing.otel-endpoint}로 일반화 예정). jar URL/path는 {@code otel-java-agent.jar-url} /
 * {@code otel-java-agent.jar-path}에서 주입.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OtelJavaConfigFacadeService {

    private final FileService fileService;

    @Value("${deploy.site-code}")
    private String deploySiteCode;

    @Value("${beyla.otel-endpoint}")
    private String otelEndpoint;

    @Value("${otel-java-agent.jar-url}")
    private String jarUrl;

    @Value("${otel-java-agent.jar-path}")
    private String jarPath;

    private final ClassPathResource otelJavaConfigTemplate =
            new ClassPathResource("otel_java_template.properties");

    /** install 시점에 호출되어, target VM에 배포할 OTel Java 환경변수 묶음을 문자열로 반환한다. */
    public String initOtelJavaConfig(String nsId, String mciId, String vmId) {
        if (!otelJavaConfigTemplate.exists()) {
            String errMsg =
                    "Invalid filePath : otelJavaConfigTemplate (otel_java_template.properties)";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        StringBuilder sb = new StringBuilder();
        fileService.appendConfig(otelJavaConfigTemplate, sb);

        String finalNsId = (nsId != null) ? nsId : "";
        String finalMciId = (mciId != null) ? mciId : "";
        String finalVmId = (vmId != null) ? vmId : "";

        return sb.toString()
                .replace("@SITE_CODE", deploySiteCode)
                .replace("@NS_ID", finalNsId)
                .replace("@MCI_ID", finalMciId)
                .replace("@VM_ID", finalVmId)
                .replace("@OTEL_ENDPOINT", otelEndpoint)
                .replace("@JAR_URL", jarUrl)
                .replace("@JAR_PATH", jarPath);
    }
}
