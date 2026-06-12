package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.service.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FluentBitConfigFacadeService {

    private final FileService fileService;
    private final InfluxDbFacadeService influxDbFacadeService;

    @Value("${loki.url}")
    private String lokiURL;

    // Optional explicit override for the host the VM's fluent-bit pushes logs to. Normally left
    // blank: the host is auto-derived from the InfluxDB endpoint this VM already uses (same host
    // as Loki, already set to the deployment's VM-reachable address). See loki.agent-host.
    @Value("${loki.agent-host:}")
    private String lokiAgentHost;

    private final ClassPathResource fluentBitVariables =
            new ClassPathResource("fluent-bit_variables");

    public String initFluentbitConfig(String nsId, String infraId, String nodeId) {
        String template = fileService.getFileContent(fluentBitVariables);

        String lokiHost = resolveLokiAgentHost(nsId, infraId);

        StringBuilder sb = new StringBuilder();

        fileService.appendConfig(fluentBitVariables, sb);

        nsId = nsId != null ? nsId : "";
        infraId = infraId != null ? infraId : "";
        nodeId = nodeId != null ? nodeId : "";
        lokiHost = lokiHost != null ? lokiHost : "";

        log.debug("VM={}/{}/{}", nsId, infraId, nodeId);

        return template.replace("@NS_ID", nsId)
                .replace("@INFRA_ID", infraId)
                .replace("@NODE_ID", nodeId)
                .replace("@LOKI_HOST", lokiHost);
    }

    /**
     * Resolves the host the VM's fluent-bit should push logs to — it must be reachable FROM the
     * target VM, so it cannot be the manager's internal docker hostname (loki.url).
     *
     * <p>Resolution order:
     *
     * <ol>
     *   <li>explicit {@code loki.agent-host} override, if set;
     *   <li>the host of the InfluxDB endpoint this VM already uses — telegraf pushes metrics there
     *       successfully, it is co-located with Loki, and the deployment (mc-admin-cli) already
     *       sets it to the VM-reachable (public) address, so log collection works automatically
     *       without extra config;
     *   <li>the host parsed from {@code loki.url} as a last resort (legacy behavior).
     * </ol>
     */
    private String resolveLokiAgentHost(String nsId, String infraId) {
        if (lokiAgentHost != null && !lokiAgentHost.isBlank()) {
            return lokiAgentHost.trim();
        }
        try {
            InfluxDTO out = influxDbFacadeService.resolveForVM(nsId, infraId);
            String host = hostOf(out.getUrl());
            if (host != null && !host.isBlank()) {
                return host;
            }
        } catch (Exception e) {
            log.warn(
                    "[FLUENTBIT] failed to derive Loki host from InfluxDB endpoint for {}/{}; "
                            + "falling back to loki.url. err={}",
                    nsId,
                    infraId,
                    e.getMessage());
        }
        return hostOf(lokiURL);
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
