package com.mcmp.o11ymanager.trigger.application.service.dto;

import com.mcmp.o11ymanager.trigger.application.common.dto.*;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerTargetUpdateRequest;
import java.util.List;
import lombok.Builder;

@Builder
public record TriggerTargetUpdateDto(List<TriggerTargetDto> triggerTargetDtos) {

    public static TriggerTargetUpdateDto from(TriggerTargetUpdateRequest req) {
        return TriggerTargetUpdateDto.builder().triggerTargetDtos(req.triggerTargets()).build();
    }
}
