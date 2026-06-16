package com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Response item from the NCP Kakao AlimTalk template-inquiry API. Maps the subset of fields the
 * application needs to validate and render a registered template; unknown fields are ignored.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoTemplateResponse {

    private String templateCode;
    private String templateName;
    private String channelId;
    private String content;
    private String templateStatus;
    private String templateInspectionStatus;
}
