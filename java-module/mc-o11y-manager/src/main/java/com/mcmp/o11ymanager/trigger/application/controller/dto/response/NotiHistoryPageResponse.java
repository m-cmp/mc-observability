package com.mcmp.o11ymanager.trigger.application.controller.dto.response;

import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.NotiHistoryDetailDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public record NotiHistoryPageResponse(
        List<NotiHistoryDetailDto> content,
        Pageable pageable,
        long totalPages,
        long totalElements,
        long numberOfElements) {

    public static NotiHistoryPageResponse from(CustomPageDto<NotiHistoryDetailDto> dto) {
        return new NotiHistoryPageResponse(
                dto.content(),
                dto.pageable(),
                dto.totalPages(),
                dto.totalElements(),
                dto.numberOfElements());
    }
}
