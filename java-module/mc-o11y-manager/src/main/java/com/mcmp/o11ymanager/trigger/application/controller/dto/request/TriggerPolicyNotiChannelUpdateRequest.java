package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelUpdateDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;


public record TriggerPolicyNotiChannelUpdateRequest(
        @NotNull @NotBlank String channelName, @NotNull @NotEmpty List<String> recipients) {

    public TriggerPolicyNotiChannelUpdateDto toDto() {
        return new TriggerPolicyNotiChannelUpdateDto(channelName, recipients);
    }
}
