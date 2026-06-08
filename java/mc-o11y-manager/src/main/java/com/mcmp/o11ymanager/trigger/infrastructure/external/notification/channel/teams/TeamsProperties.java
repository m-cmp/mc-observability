package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.teams;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.TEAMS;

import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for Microsoft Teams notifications Contains the Workflows webhook base
 * URL used as channel metadata when the channel is registered.
 */
@Getter
@Setter
public class TeamsProperties implements NotiProperty {
    private final NotificationType type = TEAMS;
    private String baseUrl;
}
