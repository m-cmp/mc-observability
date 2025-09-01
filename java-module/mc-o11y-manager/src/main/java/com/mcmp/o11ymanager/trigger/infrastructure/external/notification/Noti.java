package com.mcmp.o11ymanager.trigger.infrastructure.external.notification;


import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;

/**
 * Base interface for notification messages Represents a notification that can be sent through
 * various channels.
 */
public interface Noti {

    /**
     * Gets the type of notification channel.
     *
     * @return the notification type
     */
     NotificationType getNotificationType();
}
