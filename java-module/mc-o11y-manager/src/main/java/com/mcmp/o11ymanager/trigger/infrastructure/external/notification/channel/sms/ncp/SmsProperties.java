package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp;

import static com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType.SMS;

import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationConfigurationException;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.type.NotificationType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for NCP SMS service Contains NCP SMS API settings including
 * authentication credentials and signature generation.
 */
@Getter
@Setter
public class SmsProperties implements NotiProperty {
    private final NotificationType type = SMS;
    private String from;
    private String serviceId;
    private String accessKey;
    private String secretKey;
    private String provider;
    private String baseUrl;

    /**
     * Generates HMAC-SHA256 signature for NCP SMS API authentication.
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
            String url = "/sms/v2/services/" + serviceId + "/messages";

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
            throw new NotificationConfigurationException("Failed to generate SMS signature", e);
        }
    }

    /**
     * Constructs complete NCP SMS API URL.
     *
     * @return complete NCP SMS API URL with service ID
     */
    public String getApiUrl() {
        return baseUrl + "/sms/v2/services/" + serviceId + "/messages";
    }
}
