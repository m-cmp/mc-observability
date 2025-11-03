package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationDeliveryException;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Notifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackNoti.RequestHeader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * Slack notification sender implementation Handles sending alert notifications to Slack channels
 * via REST API.
 */
@Slf4j
@Component
public class SlackNotifier implements Notifier {

    private final RestClient restClient;

    /**
     * Constructor for SlackNotifier.
     *
     * @param restClient REST client for HTTP communication with Slack API
     */
    public SlackNotifier(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Sends notification to multiple Slack channels.
     *
     * @param noti the notification to send (must be SlackNoti)
     * @return result of the Slack notification delivery
     */
    @Override
    public NotiResult send(Noti noti) {
        List<Exception> exceptions = new ArrayList<>();
        String recipients = "";

        try {
            if (!(noti instanceof SlackNoti slackNoti)) {
                throw new InvalidNotificationTypeException(
                        "Expected SlackNoti but got: " + noti.getClass().getSimpleName());
            }

            List<String> channels = slackNoti.getRecipients();
            recipients = String.join(", ", channels);
            for (String channel : channels) {
                slackNoti.updateChannel(channel);
                RequestHeader header = slackNoti.getHeader();
                restClient
                        .post()
                        .uri(header.getUrl())
                        .header("Authorization", "Bearer " + header.getAuthorization())
                        .header("Content-Type", header.getContentType())
                        .body(slackNoti.getBody())
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                (request, response) -> {
                                    HttpStatusCode status = response.getStatusCode();
                                    String body =
                                            StreamUtils.copyToString(
                                                    response.getBody(), StandardCharsets.UTF_8);
                                    log.error(
                                            "Failed to send slack, status={}, body={}",
                                            status,
                                            body);
                                    exceptions.add(
                                            new NotificationDeliveryException(
                                                    "Slack", status.value(), body));
                                })
                        .toBodilessEntity();
            }
        } catch (Exception e) {
            return NotiResult.fail(recipients, e);
        }

        if (!exceptions.isEmpty()) {
            return NotiResult.partialFail(recipients, exceptions);
        }

        return NotiResult.success(recipients);
    }
}
