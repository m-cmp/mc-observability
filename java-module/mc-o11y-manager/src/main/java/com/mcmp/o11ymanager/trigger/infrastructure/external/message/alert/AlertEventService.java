package com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert;

import com.mcmp.o11ymanager.trigger.adapter.internal.notification.NotiServiceInternal;
import com.mcmp.o11ymanager.trigger.adapter.internal.trigger.AlertServiceInternal;
import com.mcmp.o11ymanager.trigger.adapter.internal.trigger.TriggerServiceInternal;
import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiFactory;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiSender;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service for processing alert events and coordinating alert-related operations Handles alert
 * history creation, notification sending, and test alert processing.
 */
@Slf4j
@Component
public class AlertEventService {

    private final TriggerServiceInternal triggerService;
    private final AlertServiceInternal alertService;
    private final NotiServiceInternal notiService;
    private final NotiSender notiSender;
    private final NotiFactory notiFactory;

    /**
     * Constructor for AlertEventService.
     *
     * @param alertService internal service for alert operations
     * @param triggerService internal service for trigger operations
     * @param notiService internal service for notification operations
     * @param notiSender notification sender for delivering notifications
     * @param notiFactory factory for creating notification objects
     */
    public AlertEventService(
            AlertServiceInternal alertService,
            TriggerServiceInternal triggerService,
            NotiServiceInternal notiService,
            NotiSender notiSender,
            NotiFactory notiFactory) {
        this.alertService = alertService;
        this.triggerService = triggerService;
        this.notiService = notiService;
        this.notiSender = notiSender;
        this.notiFactory = notiFactory;
    }

    /**
     * Creates trigger history from the alert event.
     *
     * @param alertEvent the alert event to record in history
     */
    public void createHistory(AlertEvent alertEvent) {
        triggerService.createTriggerHistory(alertEvent);
    }

    /**
     * Sends notifications for the alert event to configured channels. Retrieves notification
     * channels, creates notifications, sends them, and records results.
     *
     * @param alertEvent the alert event to send notifications for
     */
    public void sendNoti(AlertEvent alertEvent) {
        List<TriggerPolicyNotiChannelDto> notiChannelDetailDtos =
                notiService.getNotiChannelsBy(alertEvent.getTitle());
        List<NotiResult> notiResults = new ArrayList<>();
        for (TriggerPolicyNotiChannelDto notiChannelDto : notiChannelDetailDtos) {
            Noti noti = notiFactory.createNoti(notiChannelDto, alertEvent);
            NotiResult result = notiSender.send(noti);
            result.setChannel(notiChannelDto.name());
            notiResults.add(result);
        }
        notiService.createNotiHistory(notiResults);
    }

    /**
     * Creates test history record for test alert messages.
     *
     * @param message the test alert message to record
     */
    public void createTestHistory(String message) {
        alertService.createTestHistory(message);
    }

    /**
     * Retrieves threshold conditions for the specified trigger policy.
     *
     * @param title the trigger policy title
     * @return threshold conditions for the policy
     */
    public ThresholdCondition getThresholdCondition(String title) {
        return triggerService.getThresholdCondition(title);
    }
}
