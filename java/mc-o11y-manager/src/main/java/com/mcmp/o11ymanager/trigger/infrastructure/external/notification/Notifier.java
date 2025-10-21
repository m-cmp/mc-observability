package com.mcmp.o11ymanager.trigger.infrastructure.external.notification;

import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import java.time.Duration;
import java.util.List;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * Functional interface for sending notifications Implementations handle the actual delivery of
 * notifications through specific channels.
 */
@FunctionalInterface
public interface Notifier {
    /**
     * Sends a notification synchronously.
     *
     * @param input the notification to send
     * @return result of the notification delivery
     */
    NotiResult send(Noti input);

    /**
     * Sends a notification asynchronously with retry and timeout logic.
     *
     * @param input the notification to send
     * @return Mono containing the result of the notification delivery
     */
    default Mono<NotiResult> sendAsync(Noti input) {
        return Mono.fromCallable(() -> send(input))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .onErrorResume(Mono::error);
    }

    interface NotiFactory {
        /**
         * Gets all configured notification channel properties.
         *
         * @return list of notification channel properties
         */
        List<NotiProperty> getNotiChannelProps();

        /**
         * Creates a notification object based on channel configuration and alert event.
         *
         * @param notiChannelDto notification channel configuration
         * @param alertEvent alert event information
         * @return created notification object
         */
        Noti createNoti(TriggerPolicyNotiChannelDto notiChannelDto, AlertEvent alertEvent);
    }
}
