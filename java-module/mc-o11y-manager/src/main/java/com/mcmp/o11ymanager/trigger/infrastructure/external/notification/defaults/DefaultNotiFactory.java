package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiFactory;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoNoti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail.MailNoti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail.MailProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackNoti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsNoti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of NotiFactory Creates notification objects based on type and
 * configuration.
 */
public class DefaultNotiFactory implements NotiFactory {

    private final Map<NotificationType, NotiProperty> notiChannelProps = new HashMap<>();

    /**
     * Creates a new instance of DefaultNotiFactory.
     *
     * @return new DefaultNotiFactory instance
     */
    public static DefaultNotiFactory newInstance() {
        return new DefaultNotiFactory();
    }

    /**
     * Registers a notification property for a specific type.
     *
     * @param type the notification type
     * @param property the notification property configuration
     * @return this factory instance for method chaining
     */
    public DefaultNotiFactory put(NotificationType type, NotiProperty property) {
        notiChannelProps.put(type, property);
        return this;
    }

    /**
     * Gets all configured notification channel properties.
     *
     * @return list of non-null notification properties
     */
    public List<NotiProperty> getNotiChannelProps() {
        return notiChannelProps.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Creates a notification object based on channel configuration and alert event.
     *
     * @param notiChannelDto notification channel configuration
     * @param alertEvent alert event information
     * @return created notification object
     * @throws InvalidNotificationTypeException if the notification type is not configured
     */
    public Noti createNoti(TriggerPolicyNotiChannelDto notiChannelDto, AlertEvent alertEvent) {
        NotificationType notificationType =
                NotificationType.valueOf(notiChannelDto.type().toUpperCase());
        if (!notiChannelProps.containsKey(notificationType)) {
            throw new InvalidNotificationTypeException(
                    "Notification type " + notificationType + " is not configured");
        }

        NotiProperty notiProperty = notiChannelProps.get(notificationType);
        return switch (notificationType) {
            case SMS -> SmsNoti.from(
                    alertEvent, (SmsProperties) notiProperty, notiChannelDto.recipients());
            case EMAIL -> MailNoti.from(
                    alertEvent, (MailProperties) notiProperty, notiChannelDto.recipients());
            case SLACK -> SlackNoti.from(
                    alertEvent, (SlackProperties) notiProperty, notiChannelDto.recipients());
            case KAKAO -> KakaoNoti.from(
                    alertEvent, (KakaoProperties) notiProperty, notiChannelDto.recipients());
        };
    }

    /**
     * Marker interface for notification properties All notification channel properties should
     * implement this interface.
     */
    public interface NotiProperty {}
}
