package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiSender;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Notifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.util.HashMap;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Default implementation of NotiSender Routes notifications to appropriate notifiers based on
 * notification type.
 */
public class DefaultNotiSender implements NotiSender {

    Map<NotificationType, Notifier> notifiers = new HashMap<>();

    /**
     * Creates a new instance of DefaultNotiSender.
     *
     * @return new DefaultNotiSender instance
     */
    public static DefaultNotiSender newInstance() {
        return new DefaultNotiSender();
    }

    /**
     * Registers a notifier for a specific notification type.
     *
     * @param type the notification type
     * @param notifier the notifier implementation
     * @return this sender instance for method chaining
     */
    public DefaultNotiSender put(NotificationType type, Notifier notifier) {
        notifiers.put(type, notifier);
        return this;
    }

    /**
     * Sends a notification synchronously using the appropriate notifier.
     *
     * @param noti the notification to send
     * @return result of the notification delivery
     * @throws InvalidNotificationTypeException if no notifier is configured for the type
     */
    @Override
    public NotiResult send(Noti noti) {
        if (!notifiers.containsKey(noti.getNotificationType())) {
            throw new InvalidNotificationTypeException(
                    "No notifier configured for type: " + noti.getNotificationType());
        }
        Notifier notifier = notifiers.get(noti.getNotificationType());
        return notifier.send(noti);
    }

    /**
     * Sends a notification asynchronously using the appropriate notifier.
     *
     * @param noti the notification to send
     * @return Mono containing the result of the notification delivery
     */
    @Override
    public Mono<NotiResult> sendAsync(Noti noti) {
        Notifier notifier = notifiers.get(noti.getNotificationType());
        return notifier.sendAsync(noti);
    }
}
