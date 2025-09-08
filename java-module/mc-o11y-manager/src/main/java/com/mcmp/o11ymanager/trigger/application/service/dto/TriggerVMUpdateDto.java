package com.mcmp.o11ymanager.trigger.application.service.dto;

import com.mcmp.o11ymanager.trigger.application.common.dto.*;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.TriggerVMUpdateRequest;
import java.util.List;
import lombok.Builder;

@Builder
public record TriggerVMUpdateDto(List<TriggerVMDto> triggerVMDtos) {

    public static TriggerVMUpdateDto from(TriggerVMUpdateRequest req) {
        return TriggerVMUpdateDto.builder().triggerVMDtos(req.triggerVMs()).build();
    }
}
