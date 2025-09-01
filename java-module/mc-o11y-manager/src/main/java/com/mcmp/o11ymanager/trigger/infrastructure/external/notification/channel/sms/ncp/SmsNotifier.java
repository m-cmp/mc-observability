package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationDeliveryException;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Notifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsNoti.Message;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsNoti.RequestHeader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * SMS notification sender implementation for NCP SMS service Handles sending alert notifications
 * via NCP SMS API with signature-based authentication.
 */
@Slf4j
@Component
public class SmsNotifier implements Notifier {

    private final RestClient restClient;

    /**
     * Constructor for SmsNotifier.
     *
     * @param restClient REST client for HTTP communication with NCP SMS API
     */
    public SmsNotifier(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Sends SMS notifications to multiple recipients using NCP SMS API.
     *
     * @param noti the notification to send (must be SmsNoti)
     * @return result of the SMS notification delivery
     */
    @Override
    public NotiResult send(Noti noti) {
        String recipients = "";

        try {
            if (!(noti instanceof SmsNoti smsNoti)) {
                throw new InvalidNotificationTypeException(
                        "Expected SmsNoti but got: " + noti.getClass().getSimpleName());
            }

            RequestHeader header = smsNoti.getHeader();
            recipients =
                    smsNoti.getBody().getMessages().stream()
                            .map(Message::getTo)
                            .collect(Collectors.joining(", "));
            restClient
                    .post()
                    .uri(header.getUrl())
                    .header("x-ncp-apigw-timestamp", header.getTimestamp())
                    .header("x-ncp-iam-access-key", header.getAccessKey())
                    .header("x-ncp-apigw-signature-v2", header.getAuthorization())
                    .header("Content-Type", header.getContentType())
                    .body(smsNoti.getBody())
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                HttpStatusCode status = response.getStatusCode();
                                String body =
                                        StreamUtils.copyToString(
                                                response.getBody(), StandardCharsets.UTF_8);
                                log.error("Failed to send sms, status={}, body={}", status, body);
                                throw new NotificationDeliveryException(
                                        "SMS", status.value(), body);
                            })
                    .toBodilessEntity();
            return NotiResult.success(recipients);
        } catch (Exception e) {
            return NotiResult.fail(recipients, e);
        }
    }
}
