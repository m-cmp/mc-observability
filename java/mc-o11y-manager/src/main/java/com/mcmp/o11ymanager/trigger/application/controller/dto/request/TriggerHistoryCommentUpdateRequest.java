package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerHistoryCommentUpdateDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TriggerHistoryCommentUpdateRequest(
        @Schema(
                        description = "Comment for trigger history",
                        example = "Alert resolved - false positive")
                @NotNull @NotBlank String comment) {

    public TriggerHistoryCommentUpdateDto toDto() {
        return new TriggerHistoryCommentUpdateDto(comment);
    }
}
