package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerHistoryCommentUpdateDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TriggerHistoryCommentUpdateRequest(@NotNull @NotBlank String comment) {

    public TriggerHistoryCommentUpdateDto toDto() {
        return new TriggerHistoryCommentUpdateDto(comment);
    }
}
