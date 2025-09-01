package com.mcmp.o11ymanager.trigger.application.controller.dto.response;

import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerHistoryDetailDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public record TriggerHistoryPageResponse(
        List<TriggerHistoryDetailDto> content,
        Pageable pageable,
        long totalPages,
        long totalElements,
        long numberOfElements) {

    public static TriggerHistoryPageResponse from(CustomPageDto<TriggerHistoryDetailDto> dto) {
        return new TriggerHistoryPageResponse(
                dto.content(),
                dto.pageable(),
                dto.totalPages(),
                dto.totalElements(),
                dto.numberOfElements());
    }
}
