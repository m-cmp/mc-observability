package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.discord;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.DISCORD;

import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for Discord notifications Contains the webhook base URL and the bot
 * display settings used when sending messages.
 */
@Getter
@Setter
public class DiscordProperties implements NotiProperty {
    private final NotificationType type = DISCORD;
    private String baseUrl;
    private String username;
    private String avatarUrl;
}
