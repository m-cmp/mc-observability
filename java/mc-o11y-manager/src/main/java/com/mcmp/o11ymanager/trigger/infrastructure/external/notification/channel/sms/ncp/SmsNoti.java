package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.SMS;

import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackNoti.RequestBody;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.util.List;
import lombok.Getter;
import org.springframework.util.MimeTypeUtils;

/**
 * SMS notification data class for NCP SMS service Represents an SMS notification message formatted
 * for NCP SMS API requirements.
 */
@Getter
public class SmsNoti implements Noti {
    private static final NotificationType notiType = SMS;
    private RequestHeader header;
    private RequestBody body;

    /**
     * Creates an SmsNoti instance from alert event and SMS properties.
     *
     * @param event the alert event information
     * @param smsProperties SMS configuration properties for NCP
     * @param recipients list of phone number recipients
     * @return SmsNoti instance ready to be sent
     */
    public static Noti from(
            AlertEvent event, SmsProperties smsProperties, List<String> recipients) {
        SmsNoti notification = new SmsNoti();
        notification.header = buildHeader(smsProperties);
        notification.body = buildBody(event, smsProperties, recipients);
        return notification;
    }

    public static SmsNoti direct(
            SmsProperties smsProperties, List<String> recipients, String title, String message) {
        SmsNoti notification = new SmsNoti();
        notification.header = buildHeader(smsProperties);
        notification.body = buildDirectBody(smsProperties, recipients, title, message);
        return notification;
    }

    private static RequestBody buildDirectBody(
            SmsProperties smsProperties, List<String> recipients, String title, String message) {

        String content =
                """
        [M-CMP]
        %s
        %s
        """.formatted(title, message);

        RequestBody requestBody = new RequestBody();
        requestBody.from = smsProperties.getFrom();
        requestBody.content = content;
        requestBody.messages = recipients.stream().map(Message::new).toList();

        return requestBody;
    }

    /**
     * Builds request header for NCP SMS API call with authentication.
     *
     * @param smsProperties SMS configuration properties
     * @return RequestHeader with API URL, timestamp, and signature-based authorization
     */
    private static RequestHeader buildHeader(SmsProperties smsProperties) {
        RequestHeader requestHeader = new RequestHeader();
        requestHeader.url = smsProperties.getApiUrl();
        requestHeader.timestamp = String.valueOf(System.currentTimeMillis());
        requestHeader.authorization = smsProperties.makeSignature(requestHeader.timestamp);
        requestHeader.accessKey = smsProperties.getAccessKey();
        return requestHeader;
    }

    private static RequestBody buildBody(
            AlertEvent event, SmsProperties smsProperties, List<String> recipients) {
        String content =
                """
				[M-CMP] %s alerts triggered.

				%s
				- info: %s
				- warning: %s
				- critical: %s
				"""
                        .formatted(
                                event.getAlertsCount(),
                                event.getTitle(),
                                event.getInfoAlerts().size(),
                                event.getWarningAlerts().size(),
                                event.getCriticalAlerts().size());

        RequestBody requestBody = new RequestBody();
        requestBody.from = smsProperties.getFrom();
        requestBody.content = content;
        requestBody.messages = recipients.stream().map(Message::new).toList();

        return requestBody;
    }

    @Override
    public NotificationType getNotificationType() {
        return notiType;
    }

    @Getter
    public static class RequestHeader {
        private static final String contentType = MimeTypeUtils.APPLICATION_JSON.toString();
        private String url;
        private String timestamp;
        private String authorization;
        private String accessKey;

        public String getContentType() {
            return contentType;
        }
    }

    @Getter
    public static class RequestBody {
        private static final String type = "SMS";
        private static final String contentType = "COMM";
        private String from;
        private String content;
        private List<Message> messages;

        public String getType() {
            return type;
        }

        public String getContentType() {
            return contentType;
        }
    }

    @Getter
    public static class Message {
        private final String to;

        public Message(String to) {
            this.to = to;
        }
    }
}
