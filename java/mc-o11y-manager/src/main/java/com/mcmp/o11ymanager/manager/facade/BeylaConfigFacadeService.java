package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
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
 * <p>치환 placeholder: {@code @SITE_CODE}, {@code @NS_ID}, {@code @INFRA_ID}, {@code @NODE_ID},
 * {@code @OTEL_ENDPOINT}.
 *
 * <p>OTEL endpoint의 host는, 내부 docker 이름(otel-endpoint 기본값)이 실제 VM에서 해석되지 않으므로, 그 VM이 이미 사용하는
 * InfluxDB 엔드포인트의 host(Tempo와 동일 호스트이자 VM-도달 가능 주소)로 자동 치환한다. scheme/port는 유지. {@code
 * beyla.agent-host}로 명시적 override 가능. (FluentBitConfigFacadeService와 동일 패턴)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BeylaConfigFacadeService {

    private final FileService fileService;
    private final InfluxDbFacadeService influxDbFacadeService;

    @Value("${deploy.site-code}")
    private String deploySiteCode;

    @Value("${beyla.otel-endpoint}")
    private String otelEndpoint;

    @Value("${beyla.agent-host:}")
    private String beylaAgentHost;

    private final ClassPathResource beylaConfigTemplate =
            new ClassPathResource("beyla_template.yaml");

    /** install 시점에 호출되어, target VM에 배포할 beyla.yaml 내용을 문자열로 반환한다. */
    public String initBeylaConfig(String nsId, String infraId, String nodeId) {
        if (!beylaConfigTemplate.exists()) {
            String errMsg = "Invalid filePath : beylaConfigTemplate (beyla_template.yaml)";
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }

        StringBuilder sb = new StringBuilder();
        fileService.appendConfig(beylaConfigTemplate, sb);

        String finalNsId = (nsId != null) ? nsId : "";
        String finalInfraId = (infraId != null) ? infraId : "";
        String finalNodeId = (nodeId != null) ? nodeId : "";

        return sb.toString()
                .replace("@SITE_CODE", deploySiteCode)
                .replace("@NS_ID", finalNsId)
                .replace("@INFRA_ID", finalInfraId)
                .replace("@NODE_ID", finalNodeId)
                .replace("@OTEL_ENDPOINT", resolveOtelEndpoint(nsId, infraId));
    }

    /**
     * Rewrites the OTLP endpoint host to one reachable FROM the target VM (the internal docker
     * hostname in otel-endpoint is not), keeping the configured scheme and port.
     *
     * <p>Host resolution order: explicit {@code beyla.agent-host} → the host of the InfluxDB
     * endpoint this VM already uses (co-located with Tempo, already VM-reachable) → the original
     * otel-endpoint host (legacy).
     */
    private String resolveOtelEndpoint(String nsId, String infraId) {
        if (beylaAgentHost != null && !beylaAgentHost.isBlank()) {
            return rewriteHost(otelEndpoint, beylaAgentHost.trim());
        }
        try {
            InfluxDTO out = influxDbFacadeService.resolveForVM(nsId, infraId);
            String host = hostOf(out.getUrl());
            if (host != null && !host.isBlank()) {
                return rewriteHost(otelEndpoint, host);
            }
        } catch (Exception e) {
            log.warn(
                    "[BEYLA] failed to derive OTLP host from InfluxDB endpoint for {}/{}; "
                            + "using configured otel-endpoint. err={}",
                    nsId,
                    infraId,
                    e.getMessage());
        }
        return otelEndpoint;
    }

    /** Replaces the host of a {@code scheme://host:port/path} URL, preserving scheme/port/path. */
    private String rewriteHost(String url, String newHost) {
        if (url == null) {
            return null;
        }
        int schemeIdx = url.indexOf("://");
        String scheme = schemeIdx >= 0 ? url.substring(0, schemeIdx + 3) : "";
        String rest = schemeIdx >= 0 ? url.substring(schemeIdx + 3) : url;
        int portIdx = rest.indexOf(':');
        int pathIdx = rest.indexOf('/');
        String tail;
        if (portIdx >= 0 && (pathIdx < 0 || portIdx < pathIdx)) {
            tail = rest.substring(portIdx);
        } else if (pathIdx >= 0) {
            tail = rest.substring(pathIdx);
        } else {
            tail = "";
        }
        return scheme + newHost + tail;
    }

    /** Extracts the host portion from a {@code scheme://host:port} URL. */
    private String hostOf(String url) {
        if (url == null) {
            return null;
        }
        String[] schemeSplit = url.split("://");
        String hostPort = schemeSplit.length == 2 ? schemeSplit[1] : schemeSplit[0];
        return hostPort.split("[:/]")[0];
    }
}
