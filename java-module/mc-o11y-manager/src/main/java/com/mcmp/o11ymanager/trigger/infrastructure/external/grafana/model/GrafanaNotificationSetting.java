package com.mcmp.o11ymanager.trigger.infrastructure.external.grafana.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Grafana notification settings model for alert rule notifications Configures how and when
 * notifications are sent for alert rule triggers.
 */
@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaNotificationSetting {

    private String receiver;

    @JsonProperty("repeat_interval")
    private String repeatInterval;

    @JsonProperty("mute_time_intervals")
    private List<String> muteTimeIntervals;

    @JsonProperty("group_wait")
    private String groupWait;

    @JsonProperty("group_interval")
    private String groupInterval;

    @JsonProperty("group_by")
    private List<String> groupBy;

    @Builder
    public GrafanaNotificationSetting(
            String receiver,
            String repeatInterval,
            List<String> muteTimeIntervals,
            String groupWait,
            String groupInterval,
            List<String> groupBy) {
        this.receiver = receiver;
        this.repeatInterval = repeatInterval;
        this.muteTimeIntervals = muteTimeIntervals;
        this.groupWait = groupWait;
        this.groupInterval = groupInterval;
        this.groupBy = groupBy;
    }
}
