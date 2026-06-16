package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.KAKAO;

import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.AlertEvent;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.Noti;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.util.MimeTypeUtils;

/**
 * Kakao AlimTalk notification data class for NCP Kakao service Represents a Kakao AlimTalk
 * notification message formatted for NCP Kakao API requirements.
 */
@Getter
public class KakaoNoti implements Noti {

    private static final NotificationType notiType = KAKAO;
    private RequestHeader header;
    private RequestBody body;

    /**
     * Creates a KakaoNoti instance from alert event and Kakao properties.
     *
     * @param event the alert event information
     * @param kakaoProperties Kakao configuration properties for NCP
     * @param recipients list of phone number recipients
     * @return KakaoNoti instance ready to be sent
     */
    public static Noti from(
            AlertEvent event,
            KakaoProperties kakaoProperties,
            KakaoTemplateProvider templateProvider,
            List<String> recipients) {
        KakaoNoti notification = new KakaoNoti();
        notification.header = buildHeader(kakaoProperties);
        notification.body = buildBody(event, kakaoProperties, templateProvider, recipients);
        return notification;
    }

    public static KakaoNoti direct(
            KakaoProperties kakaoProperties,
            KakaoTemplateProvider templateProvider,
            List<String> recipients,
            String title,
            String message) {

        KakaoNoti notification = new KakaoNoti();
        notification.header = buildHeader(kakaoProperties);
        notification.body =
                buildDirectBody(kakaoProperties, templateProvider, recipients, title, message);
        return notification;
    }

    private static RequestBody buildDirectBody(
            KakaoProperties kakaoProperties,
            KakaoTemplateProvider templateProvider,
            List<String> recipients,
            String title,
            String message) {

        RequestBody requestBody = new RequestBody();
        requestBody.plusFriendId = kakaoProperties.getChannelId();
        String templateCode = kakaoProperties.getDirectTemplateCode();
        requestBody.templateCode = templateCode;

        String template = templateProvider.getContent(templateCode);
        String content =
                KakaoTemplateRenderer.render(
                        templateCode, template, Map.of("title", title, "message", message));

        requestBody.messages =
                recipients.stream().map(recipient -> new Message(recipient, content)).toList();

        return requestBody;
    }

    /**
     * Builds request header for NCP Kakao API call with authentication.
     *
     * @param kakaoProperties Kakao configuration properties
     * @return RequestHeader with API URL, timestamp, and signature-based authorization
     */
    private static RequestHeader buildHeader(KakaoProperties kakaoProperties) {
        RequestHeader requestHeader = new RequestHeader();
        requestHeader.url = kakaoProperties.getApiUrl();
        requestHeader.timestamp = String.valueOf(System.currentTimeMillis());
        requestHeader.authorization =
                kakaoProperties.makeSignature(
                        "POST", kakaoProperties.getMessagesPath(), requestHeader.timestamp);
        requestHeader.accessKey = kakaoProperties.getAccessKey();
        return requestHeader;
    }

    private static RequestBody buildBody(
            AlertEvent event,
            KakaoProperties kakaoProperties,
            KakaoTemplateProvider templateProvider,
            List<String> recipients) {
        RequestBody requestBody = new RequestBody();
        requestBody.plusFriendId = kakaoProperties.getChannelId();
        String templateCode = kakaoProperties.getAlertTemplateCode();
        requestBody.templateCode = templateCode;

        String template = templateProvider.getContent(templateCode);
        String content =
                KakaoTemplateRenderer.render(
                        templateCode, template, KakaoTemplateVariable.toValues(event));

        requestBody.messages =
                recipients.stream().map(recipient -> new Message(recipient, content)).toList();
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

        private String plusFriendId;
        private String templateCode;
        private List<Message> messages;
    }

    @Getter
    public static class Message {

        private final String to;
        private final String content;

        public Message(String to, String content) {
            this.to = to;
            this.content = content;
        }
    }
}
