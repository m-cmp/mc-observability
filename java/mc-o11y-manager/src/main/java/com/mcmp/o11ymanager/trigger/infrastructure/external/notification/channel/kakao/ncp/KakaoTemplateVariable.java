package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalog of variables supported in the Kakao AlimTalk alert template. Each entry maps a {@code
 * #{key}} placeholder to the value extracted from an {@link AlertEvent}. This enum is the single
 * source of truth for which alert variables exist and is mirrored in user-facing setup docs.
 */
public enum KakaoTemplateVariable {
    POLICY_NAME("policyName", "트리거 정책명 (Grafana rule group)", AlertEvent::getTitle),
    TOTAL_COUNT("totalCount", "전체 알림 건수", event -> String.valueOf(event.getAlertsCount())),
    INFO_COUNT("infoCount", "INFO 단계 알림 건수", event -> String.valueOf(event.getInfoAlerts().size())),
    WARNING_COUNT(
            "warningCount",
            "WARNING 단계 알림 건수",
            event -> String.valueOf(event.getWarningAlerts().size())),
    CRITICAL_COUNT(
            "criticalCount",
            "CRITICAL 단계 알림 건수",
            event -> String.valueOf(event.getCriticalAlerts().size()));

    private final String key;
    private final String description;
    private final Function<AlertEvent, String> extractor;

    KakaoTemplateVariable(
            String key, String description, Function<AlertEvent, String> extractor) {
        this.key = key;
        this.description = description;
        this.extractor = extractor;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Builds the placeholder-to-value map for the given alert event.
     *
     * @param event the alert event to extract values from
     * @return ordered map of variable key to resolved value
     */
    public static Map<String, String> toValues(AlertEvent event) {
        Map<String, String> values = new LinkedHashMap<>();
        for (KakaoTemplateVariable variable : values()) {
            values.put(variable.key, variable.extractor.apply(event));
        }
        return values;
    }

    /**
     * Returns all supported alert variable keys.
     *
     * @return ordered set of variable keys
     */
    public static Set<String> keys() {
        return Arrays.stream(values())
                .map(variable -> variable.key)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
