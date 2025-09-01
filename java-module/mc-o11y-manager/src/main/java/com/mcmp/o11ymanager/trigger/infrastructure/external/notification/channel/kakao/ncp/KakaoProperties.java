package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.KAKAO;

import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationConfigurationException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for NCP Kakao FriendTalk service Contains NCP Kakao API settings
 * including channel configuration and signature generation.
 */
@Getter
@Setter
public class KakaoProperties implements NotiProperty {
    private final NotificationType type = KAKAO;
    private String channelId;
    private String templateCode;
    private String serviceId;
    private String accessKey;
    private String secretKey;
    private String provider;
    private String baseUrl;

    /**
     * Generates HMAC-SHA256 signature for NCP Kakao API authentication.
     *
     * @param timestamp current timestamp for signature generation
     * @return Base64-encoded signature for API authentication
     * @throws NotificationConfigurationException if signature generation fails
     */
    public String makeSignature(String timestamp) {
        try {
            String space = " ";
            String newLine = "\n";
            String method = "POST";
            String url = "/friendtalk/v2/services/" + serviceId + "/messages";

            String message =
                method
                    + space
                    + url
                    + newLine
                    + timestamp
                    + newLine
                    + accessKey;

            SecretKeySpec signingKey =
                    new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new NotificationConfigurationException("Failed to generate Kakao signature", e);
        }
    }

    /**
     * Constructs complete NCP Kakao FriendTalk API URL.
     *
     * @return complete NCP Kakao FriendTalk API URL with service ID
     */
    public String getApiUrl() {
        return baseUrl + "/friendtalk/v2/services/" + serviceId + "/messages";
    }
}
