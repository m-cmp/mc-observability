package com.mcmp.o11ymanager.trigger.application.service.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public record TriggerPolicyNotiChannelDto(
        long id,
        String name,
        String type,
        String provider,
        String baseUrl,
        boolean isActive,
        List<String> recipients) {

    public TriggerPolicyNotiChannelDto {
        if (recipients == null) {
            recipients = new ArrayList<>();
        }
    }
}
