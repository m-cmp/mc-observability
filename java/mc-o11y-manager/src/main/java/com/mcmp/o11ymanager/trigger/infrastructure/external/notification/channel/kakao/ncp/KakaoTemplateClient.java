package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import com.mcmp.o11ymanager.trigger.application.common.exception.NotificationConfigurationException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * Client for the NCP Kakao AlimTalk template-inquiry API. Looks up a registered template by code so
 * the application can validate its approval status and read its content.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoTemplateClient {

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

    /**
     * Looks up a single registered template by its code.
     *
     * @param templateCode the NCP template code to inquire
     * @return the matching template, or empty if NCP returns no template for the code
     * @throws NotificationConfigurationException if the inquiry request fails
     */
    public Optional<KakaoTemplateResponse> findTemplate(String templateCode) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String query =
                "?channelId="
                        + encode(kakaoProperties.getChannelId())
                        + "&templateCode="
                        + encode(templateCode);
        String path = kakaoProperties.getTemplatesPath() + query;
        String signature = kakaoProperties.makeSignature("GET", path, timestamp);

        List<KakaoTemplateResponse> templates =
                restClient
                        .get()
                        .uri(URI.create(kakaoProperties.getBaseUrl() + path))
                        .header("x-ncp-apigw-timestamp", timestamp)
                        .header("x-ncp-iam-access-key", kakaoProperties.getAccessKey())
                        .header("x-ncp-apigw-signature-v2", signature)
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                (request, response) -> {
                                    String body =
                                            StreamUtils.copyToString(
                                                    response.getBody(), StandardCharsets.UTF_8);
                                    log.error(
                                            "Failed to fetch Kakao template, code={}, status={}, body={}",
                                            templateCode,
                                            response.getStatusCode(),
                                            body);
                                    throw new NotificationConfigurationException(
                                            "Failed to fetch Kakao template '"
                                                    + templateCode
                                                    + "': "
                                                    + body);
                                })
                        .body(new ParameterizedTypeReference<List<KakaoTemplateResponse>>() {});

        if (templates == null) {
            return Optional.empty();
        }
        return templates.stream()
                .filter(template -> templateCode.equals(template.getTemplateCode()))
                .findFirst();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
