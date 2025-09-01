package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Grafana alert rule model representing the ProvisionedAlertRule schema Maps directly to Grafana's
 * ProvisionedAlertRule API specification for alert rule configuration. Contains all required fields
 * for creating and managing alert rules in Grafana.
 *
 * @see <a
 *     href="https://editor.swagger.io/?url=https://raw.githubusercontent.com/grafana/grafana/main/public/openapi3.json">Grafana
 *     OpenAPI Documentation</a>
 */
@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaAlertRule {
    private Integer id;

    @Getter private String uid;

    private Integer orgId;

    @JsonProperty("folderUID")
    private String folderUid;

    private String ruleGroup;

    private String title;

    private String condition;

    private List<GrafanaAlertQuery> data;

    private String updated;

    private String noDataState;

    private String execErrState;

    @JsonProperty("for")
    private String duration;

    private Map<String, String> annotations;

    private Map<String, String> labels;

    private String provenance;

    private boolean isPaused;

    @JsonProperty("notification_settings")
    private GrafanaNotificationSetting notificationSettings;

    private GrafanaRecord record;

    @Builder
    public GrafanaAlertRule(
            Integer id,
            String uid,
            Integer orgId,
            String folderUid,
            String ruleGroup,
            String title,
            String condition,
            List<GrafanaAlertQuery> data,
            String updated,
            String noDataState,
            String execErrState,
            String duration,
            Map<String, String> annotations,
            Map<String, String> labels,
            String provenance,
            boolean isPaused,
            GrafanaNotificationSetting notificationSettings,
            GrafanaRecord record) {
        this.id = id;
        this.uid = uid;
        this.orgId = orgId;
        this.folderUid = folderUid;
        this.ruleGroup = ruleGroup;
        this.title = title;
        this.condition = condition;
        this.data = data;
        this.updated = updated;
        this.noDataState = noDataState;
        this.execErrState = execErrState;
        this.duration = duration;
        this.annotations = annotations;
        this.labels = labels;
        this.provenance = provenance;
        this.isPaused = isPaused;
        this.notificationSettings = notificationSettings;
        this.record = record;
    }
}
