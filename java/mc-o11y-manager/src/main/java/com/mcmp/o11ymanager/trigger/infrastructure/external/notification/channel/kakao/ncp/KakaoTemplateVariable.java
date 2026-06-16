package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent.AlertDetail;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalog of variables supported in the Kakao AlimTalk alert template. Each entry maps a {@code
 * #{key}} placeholder to the value extracted from an {@link AlertEvent}. This enum is the single
 * source of truth for which alert variables exist and is mirrored in user-facing setup docs.
 *
 * <p>{@code scope}/{@code targetId} describe a single resource, so for an event that aggregates
 * multiple resources they resolve to the highest-severity (representative) alert.
 */
public enum KakaoTemplateVariable {
    TITLE("title", "트리거 정책명 (Grafana rule group)", AlertEvent::getTitle),
    ALERT_COUNTS("alertCounts", "전체 알림 건수", event -> String.valueOf(event.getAlertsCount())),
    INFO_COUNT("infoCount", "INFO 단계 알림 건수", event -> String.valueOf(event.getInfoAlerts().size())),
    WARNING_COUNT(
            "warningCount",
            "WARNING 단계 알림 건수",
            event -> String.valueOf(event.getWarningAlerts().size())),
    CRITICAL_COUNT(
            "criticalCount",
            "CRITICAL 단계 알림 건수",
            event -> String.valueOf(event.getCriticalAlerts().size())),
    SCOPE(
            "scope",
            "대표(최고 심각도) 알림의 네임스페이스 ID",
            event -> primaryDetail(event, AlertDetail::getNamespaceId)),
    TARGET_ID(
            "targetId",
            "대표(최고 심각도) 알림의 VM ID",
            event -> primaryDetail(event, AlertDetail::getVmId));

    private final String key;
    private final String description;
    private final Function<AlertEvent, String> extractor;

    KakaoTemplateVariable(String key, String description, Function<AlertEvent, String> extractor) {
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

    /**
     * Resolves a single-resource field from the highest-severity alert in the event (CRITICAL &gt;
     * WARNING &gt; INFO), or an empty string when the event has no alerts.
     *
     * @param event the alert event
     * @param field the {@link AlertDetail} field accessor
     * @return the field value of the representative alert, or {@code ""} if none
     */
    private static String primaryDetail(AlertEvent event, Function<AlertDetail, String> field) {
        List<AlertDetail> details =
                !event.getCriticalAlerts().isEmpty()
                        ? event.getCriticalAlerts()
                        : !event.getWarningAlerts().isEmpty()
                                ? event.getWarningAlerts()
                                : event.getInfoAlerts();
        if (details.isEmpty()) {
            return "";
        }
        String value = field.apply(details.get(0));
        return value == null ? "" : value;
    }
}
