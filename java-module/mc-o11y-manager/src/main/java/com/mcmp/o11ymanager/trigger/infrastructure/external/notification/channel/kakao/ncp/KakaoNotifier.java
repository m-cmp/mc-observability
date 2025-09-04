package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidNotificationTypeException;
import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationDeliveryException;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.NotiResult;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Notifier;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoNoti.Message;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoNoti.RequestHeader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * Kakao FriendTalk notification sender implementation for NCP Kakao service Handles sending alert
 * notifications via NCP Kakao FriendTalk API with signature-based authentication.
 */
@Slf4j
@Component
public class KakaoNotifier implements Notifier {

    private final RestClient restClient;

    /**
     * Constructor for KakaoNotifier.
     *
     * @param restClient REST client for HTTP communication with NCP Kakao API
     */
    public KakaoNotifier(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Sends Kakao FriendTalk notifications to multiple recipients using NCP API.
     *
     * @param noti the notification to send (must be KakaoNoti)
     * @return result of the Kakao notification delivery
     */
    @Override
    public NotiResult send(Noti noti) {
        String recipients = "";

        try {
            if (!(noti instanceof KakaoNoti kakaoNoti)) {
                throw new InvalidNotificationTypeException(
                        "Expected KakaoNoti but got: " + noti.getClass().getSimpleName());
            }

            RequestHeader header = kakaoNoti.getHeader();
            recipients =
                    kakaoNoti.getBody().getMessages().stream()
                            .map(Message::getTo)
                            .collect(Collectors.joining(", "));
            restClient
                    .post()
                    .uri(header.getUrl())
                    .header("x-ncp-apigw-timestamp", header.getTimestamp())
                    .header("x-ncp-iam-access-key", header.getAccessKey())
                    .header("x-ncp-apigw-signature-v2", header.getAuthorization())
                    .header("Content-Type", header.getContentType())
                    .body(kakaoNoti.getBody())
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
                                        "Kakao", status.value(), body);
                            })
                    .toBodilessEntity();

            return NotiResult.success(recipients);
        } catch (Exception e) {
            return NotiResult.fail(recipients, e);
        }
    }
}
