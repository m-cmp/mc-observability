package com.mcmp.o11ymanager.trigger.adapter.internal.notification.dto;

import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.kakao.ncp.KakaoProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.mail.MailProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.slack.SlackProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.channel.sms.ncp.SmsProperties;
import com.mcmp.o11ymanager.trigger.infrastructure.external.notification.defaults.DefaultNotiFactory.NotiProperty;
import lombok.Builder;

@Builder
public record NotiChannelCreateDto(
        String name, String type, String provider, String baseUrl, boolean isActive) {

    public static NotiChannelCreateDto from(NotiProperty channel) {
        if (channel instanceof MailProperties mailChannel) {
            return NotiChannelCreateDto.builder()
                    .name(mailChannel.getType().name().toLowerCase() + "_" + mailChannel.getHost())
                    .type(mailChannel.getType().name().toLowerCase())
                    .provider(mailChannel.getHost())
                    .baseUrl(null)
                    .isActive(true)
                    .build();
        } else if (channel instanceof SmsProperties smsChannel) {
            return NotiChannelCreateDto.builder()
                    .name(
                            smsChannel.getType().name().toLowerCase()
                                    + "_"
                                    + smsChannel.getProvider())
                    .type(smsChannel.getType().name().toLowerCase())
                    .provider(smsChannel.getProvider())
                    .baseUrl(smsChannel.getBaseUrl())
                    .isActive(true)
                    .build();
        } else if (channel instanceof KakaoProperties kakaoChannel) {
            return NotiChannelCreateDto.builder()
                    .name(
                            kakaoChannel.getType().name().toLowerCase()
                                    + "_"
                                    + kakaoChannel.getProvider())
                    .type(kakaoChannel.getType().name().toLowerCase())
                    .provider(kakaoChannel.getProvider())
                    .baseUrl(kakaoChannel.getBaseUrl())
                    .isActive(true)
                    .build();
        } else if (channel instanceof SlackProperties slackProperties) {
            return NotiChannelCreateDto.builder()
                    .name(slackProperties.getType().name().toLowerCase())
                    .type(slackProperties.getType().name().toLowerCase())
                    .provider(slackProperties.getType().name().toLowerCase())
                    .baseUrl(slackProperties.getBaseUrl())
                    .isActive(true)
                    .build();
        } else {
            throw new IllegalArgumentException("Unknown channel type");
        }
    }
}
