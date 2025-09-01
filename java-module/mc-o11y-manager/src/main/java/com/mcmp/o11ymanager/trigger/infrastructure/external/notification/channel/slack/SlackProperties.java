package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack;


import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.SLACK;

import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for Slack notifications Contains Slack API settings including
 * authentication token and endpoint configuration.
 */
@Getter
@Setter
public class SlackProperties implements NotiProperty {
    private final NotificationType type = SLACK;
    private String token;
    private String baseUrl;
    private String apiPath;

    /**
     * Constructs complete Slack API URL by combining base URL and API path.
     *
     * @return complete Slack API URL
     */
    public String getApiUrl() {
        return baseUrl + apiPath;
    }
}
