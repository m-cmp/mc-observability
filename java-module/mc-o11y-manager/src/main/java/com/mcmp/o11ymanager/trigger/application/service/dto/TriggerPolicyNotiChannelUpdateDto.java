package com.mcmp.o11ymanager.trigger.application.service.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record TriggerPolicyNotiChannelUpdateDto(String channelName, List<String> recipients) {}
