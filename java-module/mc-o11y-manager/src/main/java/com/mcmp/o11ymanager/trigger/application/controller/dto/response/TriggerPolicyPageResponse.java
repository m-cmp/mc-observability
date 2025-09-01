package com.mcmp.o11ymanager.trigger.application.controller.dto.response;

import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyDetailDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public record TriggerPolicyPageResponse(
        List<TriggerPolicyDetailDto> content,
        Pageable pageable,
        long totalPages,
        long totalElements,
        long numberOfElements) {
    public static TriggerPolicyPageResponse from(CustomPageDto<TriggerPolicyDetailDto> dto) {
        return new TriggerPolicyPageResponse(
                dto.content(),
                dto.pageable(),
                dto.totalPages(),
                dto.totalElements(),
                dto.numberOfElements());
    }
}
