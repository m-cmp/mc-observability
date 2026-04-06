package com.mcmp.o11ymanager.trigger.application.controller;

import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.NotiChannelAllResponse;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.NotiHistoryPageResponse;
import com.mcmp.o11ymanager.trigger.application.service.NotiService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.NotiChannelDetailDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.NotiHistoryDetailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for notification channel and notification history management Provides
 * functionality for retrieving notification channels and notification delivery history.
 */
@Tag(name = "[Trigger] Notification")
@RestController
@RequestMapping("/api/o11y/trigger/noti")
public class NotiController {

    private final NotiService notiService;

    /**
     * Constructor for NotiController
     *
     * @param notiService Service that handles notification channel and history related business
     *     logic
     */
    public NotiController(NotiService notiService) {
        this.notiService = notiService;
    }

    /**
     * Retrieves all available notification channels.
     *
     * @return List of notification channels
     */
    @Operation(
            summary = "GetSupportedNotificationChannels",
            description = "Get supported notification channels",
            operationId = "GetSupportedNotificationChannels")
    @GetMapping("/channel")
    public ResBody<NotiChannelAllResponse> getNotiChannels() {
        List<NotiChannelDetailDto> notiChannels = notiService.getNotiChannels();
        return new ResBody<>(NotiChannelAllResponse.from(notiChannels));
    }

    /**
     * Retrieves notification delivery history with pagination.
     *
     * @param page Page number (default: 1)
     * @param size Page size (default: 20)
     * @param sortBy Field to sort by (default: createdAt)
     * @param sortDirection Sort direction (default: desc)
     * @return Notification delivery history list with paging information
     */
    @Operation(
            summary = "GetPaginatedNotificationHistories",
            description = "Get paginated notification histories",
            operationId = "GetPaginatedNotificationHistories")
    @GetMapping("/history")
    public ResBody<NotiHistoryPageResponse> getNotiHistories(
            @Parameter(description = "page number (1 .. N)") @RequestParam(defaultValue = "1")
                    int page,
            @Parameter(description = "size of page (1 .. N)") @RequestParam(defaultValue = "20")
                    int size,
            @Parameter(description = "sort by properties(id..)")
                    @RequestParam(defaultValue = "createdAt")
                    String sortBy,
            @Parameter(description = "sort direction (asc, desc)")
                    @RequestParam(defaultValue = "desc")
                    String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));

        CustomPageDto<NotiHistoryDetailDto> dto = notiService.getNotiHistories(pageable);
        return new ResBody<>(NotiHistoryPageResponse.from(dto));
    }
}
