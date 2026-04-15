package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.service.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Beyla agent의 동적 config 생성 담당.
 *
 * <p>Telegraf 패턴(TelegrafConfigFacadeService)을 모방하되, Beyla는 metric 종류 선택이 없는 단일 yaml 파일이라
 * monolithic 템플릿(FluentBit 패턴) 으로 단순화. ClassPath의 {@code beyla_template.yaml}을 읽어 placeholder를 치환한
 * 결과를 반환한다.
 *
 * <p>치환 placeholder: {@code @SITE_CODE}, {@code @NS_ID}, {@code @MCI_ID}, {@code @VM_ID},
 * {@code @OTEL_ENDPOINT}.
 *
 * <p>OTEL endpoint는 application.yaml의 {@code beyla.otel-endpoint}에서 주입 (사이트별 환경변수 {@code
 * BEYLA_OTEL_ENDPOINT}로 override 가능). InfluxDbFacadeService 같은 DB 조회 없이 단일 전역 endpoint 사용.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BeylaConfigFacadeService {

    private final FileService fileService;

    @Value("${deploy.site-code}")
    private String deploySiteCode;

    @Value("${beyla.otel-endpoint}")
    private String otelEndpoint;

    private final ClassPathResource beylaConfigTemplate =
            new ClassPathResource("beyla_template.yaml");

    /** install 시점에 호출되어, target VM에 배포할 beyla.yaml 내용을 문자열로 반환한다. */
    public String initBeylaConfig(String nsId, String mciId, String vmId) {
        if (!beylaConfigTemplate.exists()) {
            String errMsg = "Invalid filePath : beylaConfigTemplate (beyla_template.yaml)";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        StringBuilder sb = new StringBuilder();
        fileService.appendConfig(beylaConfigTemplate, sb);

        String finalNsId = (nsId != null) ? nsId : "";
        String finalMciId = (mciId != null) ? mciId : "";
        String finalVmId = (vmId != null) ? vmId : "";

        return sb.toString()
                .replace("@SITE_CODE", deploySiteCode)
                .replace("@NS_ID", finalNsId)
                .replace("@MCI_ID", finalMciId)
                .replace("@VM_ID", finalVmId)
                .replace("@OTEL_ENDPOINT", otelEndpoint);
    }
}
