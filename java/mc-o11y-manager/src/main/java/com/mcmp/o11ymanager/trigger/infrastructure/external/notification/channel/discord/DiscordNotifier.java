package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.discord;

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
 * Discord notification sender implementation Handles sending alert notifications to Discord
 * channels via incoming webhook URLs.
 */
@Slf4j
@Component
public class DiscordNotifier implements Notifier {

    private final RestClient restClient;

    /**
     * Constructor for DiscordNotifier.
     *
     * @param restClient REST client for HTTP communication with Discord webhooks
     */
    public DiscordNotifier(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Sends notification to multiple Discord webhook URLs.
     *
     * @param noti the notification to send (must be DiscordNoti)
     * @return result of the Discord notification delivery
     */
    @Override
    public NotiResult send(Noti noti) {
        List<Exception> exceptions = new ArrayList<>();
        String recipients = "";

        try {
            if (!(noti instanceof DiscordNoti discordNoti)) {
                throw new InvalidNotificationTypeException(
                        "Expected DiscordNoti but got: " + noti.getClass().getSimpleName());
            }

            List<String> webhookUrls = discordNoti.getRecipients();
            recipients =
                    webhookUrls.stream()
                            .map(DiscordNotifier::maskWebhookUrl)
                            .collect(Collectors.joining(", "));
            for (String webhookUrl : webhookUrls) {
                restClient
                        .post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(discordNoti.getBody())
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                (request, response) -> {
                                    HttpStatusCode status = response.getStatusCode();
                                    String body =
                                            StreamUtils.copyToString(
                                                    response.getBody(), StandardCharsets.UTF_8);
                                    log.error(
                                            "Failed to send discord, url={}, status={}, body={}",
                                            maskWebhookUrl(webhookUrl),
                                            status,
                                            body);
                                    exceptions.add(
                                            new NotificationDeliveryException(
                                                    "Discord", status.value(), body));
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
        int lastSlash = webhookUrl.lastIndexOf('/');
        return lastSlash < 0 ? "***" : webhookUrl.substring(0, lastSlash + 1) + "***";
    }
}
