package com.mcmp.o11ymanager.trigger.infrastructure.external.notification;

import reactor.core.publisher.Mono;

/**
 * Interface for managing notification sending operations Coordinates the delivery of notifications
 * through appropriate channels.
 */
public interface NotiSender {

    /**
     * Sends a notification synchronously.
     *
     * @param noti the notification to send
     * @return result of the notification delivery
     */
    NotiResult send(Noti noti);

    /**
     * Sends a notification asynchronously.
     *
     * @param noti the notification to send
     * @return Mono containing the result of the notification delivery
     */
    Mono<NotiResult> sendAsync(Noti noti);
}
