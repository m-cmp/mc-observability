package com.mcmp.o11ymanager.trigger.application.controller.dto.response;

import com.mcmp.o11ymanager.trigger.application.service.dto.AlertTestHistoryDetailDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public record AlertTestHistoryPageResponse(
        List<AlertTestHistoryDetailDto> content,
        Pageable pageable,
        long totalPages,
        long totalElements,
        long numberOfElements) {
    public static AlertTestHistoryPageResponse from(CustomPageDto<AlertTestHistoryDetailDto> dto) {
        return new AlertTestHistoryPageResponse(
                dto.content(),
                dto.pageable(),
                dto.totalPages(),
                dto.totalElements(),
                dto.numberOfElements());
    }
}
