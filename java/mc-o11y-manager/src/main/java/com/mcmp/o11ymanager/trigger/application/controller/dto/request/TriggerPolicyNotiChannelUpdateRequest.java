package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyNotiChannelUpdateDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TriggerPolicyNotiChannelUpdateRequest(
        @Schema(description = "Notification channel name", example = "email_smtp.gmail.com")
                @NotNull @NotBlank String channelName,
        @NotNull @NotEmpty List<String> recipients) {

    public TriggerPolicyNotiChannelUpdateDto toDto() {
        return new TriggerPolicyNotiChannelUpdateDto(channelName, recipients);
    }
}
