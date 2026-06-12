package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.persistence.model.DirectAlert;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request to send a test notification to a single channel. {@code channelName} is the short channel
 * name (sms, email, slack, kakao, discord, teams).
 */
public record DirectAlertTestRequest(
        @Schema(description = "Channel short name", example = "slack") @NotNull @NotBlank String channelName,
        @Schema(description = "Recipients for the channel") @NotNull @NotEmpty List<String> recipients,
        @Schema(description = "Optional title") String title,
        @Schema(description = "Optional message body") String message) {

    public DirectAlert toDirectAlert() {
        String t = (title == null || title.isBlank()) ? "Test notification" : title;
        String m =
                (message == null || message.isBlank())
                        ? "This is a test alert from mc-observability."
                        : message;
        return new DirectAlert(t, m, channelName, recipients);
    }
}
