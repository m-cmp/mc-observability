package com.mcmp.o11ymanager.trigger.application.controller;

import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.trigger.application.controller.dto.request.DirectAlertTestRequest;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.NotiChannelAllResponse;
import com.mcmp.o11ymanager.trigger.application.controller.dto.response.NotiHistoryPageResponse;
import com.mcmp.o11ymanager.trigger.application.service.NotiService;
import com.mcmp.o11ymanager.trigger.application.service.dto.CustomPageDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.NotiChannelDetailDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.NotiHistoryDetailDto;
import com.mcmp.o11ymanager.trigger.infrastructure.external.message.alert.DirectAlertPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final DirectAlertPublisher directAlertPublisher;

    /**
     * Constructor for NotiController
     *
     * @param notiService Service that handles notification channel and history related business
     *     logic
     * @param directAlertPublisher Publisher for sending test notifications through RabbitMQ
     */
    public NotiController(NotiService notiService, DirectAlertPublisher directAlertPublisher) {
        this.notiService = notiService;
        this.directAlertPublisher = directAlertPublisher;
    }

    /**
     * Sends a test notification to a single channel through RabbitMQ (alert-manual queue). The
     * message is processed by the normal consumer, delivered to the channel, and recorded in the
     * notification history.
     *
     * @param request channel name, recipients, and optional title/message
     * @return empty response (delivery result appears in the notification history)
     */
    @Operation(
            summary = "SendTestNotification",
            description = "Send a test notification to a channel via RabbitMQ",
            operationId = "SendTestNotification")
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResBody<Void> sendTestNotification(@Valid @RequestBody DirectAlertTestRequest request) {
        directAlertPublisher.publish(request.toDirectAlert());
        return new ResBody<>();
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
