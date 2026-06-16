package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationConfigurationException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provides registered NCP Kakao AlimTalk template content by code. Fetches and validates a template
 * from NCP on first access (approval status and non-empty content) and caches the result so the
 * inquiry API is not called on every send.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoTemplateProvider {

    private static final Set<String> USABLE_STATUSES = Set.of("ACTIVE", "READY");

    private final KakaoTemplateClient templateClient;
    private final ConcurrentHashMap<String, String> contentCache = new ConcurrentHashMap<>();

    /**
     * Returns the registered template content for the given code, fetching and validating it from
     * NCP on first access and caching the result.
     *
     * @param templateCode the NCP template code
     * @return the registered template content (with {@code #{...}} placeholders)
     * @throws NotificationConfigurationException if the template is missing, not usable, or empty
     */
    public String getContent(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new NotificationConfigurationException("Kakao templateCode is not configured");
        }
        return contentCache.computeIfAbsent(templateCode, this::fetchValidated);
    }

    /**
     * Clears the cached content for a template code so the next access re-fetches from NCP.
     *
     * @param templateCode the NCP template code to evict
     */
    public void evict(String templateCode) {
        contentCache.remove(templateCode);
    }

    private String fetchValidated(String templateCode) {
        KakaoTemplateResponse template =
                templateClient
                        .findTemplate(templateCode)
                        .orElseThrow(
                                () ->
                                        new NotificationConfigurationException(
                                                "Kakao template not found on NCP: "
                                                        + templateCode));

        String status = template.getTemplateStatus();
        if (status == null || USABLE_STATUSES.stream().noneMatch(s -> s.equalsIgnoreCase(status))) {
            throw new NotificationConfigurationException(
                    "Kakao template '"
                            + templateCode
                            + "' is not usable (templateStatus="
                            + status
                            + ", inspection="
                            + template.getTemplateInspectionStatus()
                            + ")");
        }

        String content = template.getContent();
        if (content == null || content.isBlank()) {
            throw new NotificationConfigurationException(
                    "Kakao template '" + templateCode + "' has empty content");
        }

        log.info("[KAKAO-TEMPLATE] loaded template '{}' from NCP", templateCode);
        return content;
    }
}
