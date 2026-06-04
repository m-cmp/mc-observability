package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.teams;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationDeliveryException;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Notifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * Microsoft Teams notification sender implementation Handles sending alert notifications to Teams
 * channels via Workflows incoming webhook URLs.
 */
@Slf4j
@Component
public class TeamsNotifier implements Notifier {

    private final RestClient restClient;

    /**
     * Constructor for TeamsNotifier.
     *
     * @param restClient REST client for HTTP communication with Teams webhooks
     */
    public TeamsNotifier(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Sends notification to multiple Teams webhook URLs.
     *
     * @param noti the notification to send (must be TeamsNoti)
     * @return result of the Teams notification delivery
     */
    @Override
    public NotiResult send(Noti noti) {
        List<Exception> exceptions = new ArrayList<>();
        String recipients = "";

        try {
            if (!(noti instanceof TeamsNoti teamsNoti)) {
                throw new InvalidNotificationTypeException(
                        "Expected TeamsNoti but got: " + noti.getClass().getSimpleName());
            }

            List<String> webhookUrls = teamsNoti.getRecipients();
            recipients =
                    webhookUrls.stream()
                            .map(TeamsNotifier::maskWebhookUrl)
                            .collect(Collectors.joining(", "));
            for (String webhookUrl : webhookUrls) {
                restClient
                        .post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(teamsNoti.getBody())
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                (request, response) -> {
                                    HttpStatusCode status = response.getStatusCode();
                                    String body =
                                            StreamUtils.copyToString(
                                                    response.getBody(), StandardCharsets.UTF_8);
                                    log.error(
                                            "Failed to send teams, url={}, status={}, body={}",
                                            maskWebhookUrl(webhookUrl),
                                            status,
                                            body);
                                    exceptions.add(
                                            new NotificationDeliveryException(
                                                    "Teams", status.value(), body));
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

    private static String maskWebhookUrl(String webhookUrl) {
        if (webhookUrl == null) {
            return "";
        }
        int queryIndex = webhookUrl.indexOf('?');
        return queryIndex < 0 ? webhookUrl : webhookUrl.substring(0, queryIndex) + "?***";
    }
}
