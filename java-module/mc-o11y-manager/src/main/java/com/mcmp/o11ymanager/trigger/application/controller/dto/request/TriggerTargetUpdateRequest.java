package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerTargetDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerTargetUpdateDto;
import java.util.List;

public record TriggerTargetUpdateRequest(List<TriggerTargetDto> triggerTargets) {

    public TriggerTargetUpdateDto toDto() {
        return new TriggerTargetUpdateDto(triggerTargets);
    }
}
